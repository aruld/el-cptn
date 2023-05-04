const cache = require('./cache');
const pgPool = require('./database');
const Destination = require('./entities/Destination');
const Pipeline = require('./entities/Pipeline');
const { runStep, getDestinationWrappedObject } = require('./stepRunner');
const Transformation = require('./entities/Transformation');
const getVM = require('./vm');
const logger = require('./logger');

async function getTransformation(transformationId) {
    const key = `transformation-proc::${transformationId}`;
    const cached = await cache.get(key);
    if (cached) {
        return new Transformation(cached);
    }

    const result = await pgPool.query('SELECT t.id, t.script, t.version, t.active FROM transformation t WHERE t.id=$1', [transformationId]);
    const row = result.rows?.[0];
    if (row) {
        await cache.set(key, row);
        return new Transformation(row);
    }
    return null;
}

async function getDestination(destinationId) {
    const key = `destination-proc::${destinationId}`;
    const cached = await cache.get(key);
    if (cached) {
        return new Destination(cached);
    }

    const result = await pgPool.query('SELECT d.id, d.script, d.version, d.active, d.config FROM destination d WHERE d.id=$1', [destinationId]);
    const row = result.rows?.[0];
    if (row) {
        await cache.set(key, row);
        return new Destination(row);
    }
    return null;
}

async function getPipeline(pipelineId) {
    const key = `pipeline-proc::${pipelineId}`;
    const cached = await cache.get(key);
    if (cached) {
        return new Pipeline(cached);
    }

    const result = await pgPool.query('SELECT p.id, p.source_id, p.destination_id, p.active, p.route FROM pipeline p WHERE p.id=$1', [pipelineId]);
    const row = result.rows?.[0];
    if (row) {
        await cache.set(key, row);
        return new Pipeline(row);
    }
    return null;
}

async function resolveSteps(pipeline) {
    const transformations = pipeline.transformations;
    const destinationId = pipeline.destinationId;
    let steps;
    if (transformations) {
        const stepsPromises = transformations.map(id => getTransformation(id));
        steps = await Promise.all(stepsPromises);
    } else {
        steps = [];
    }

    const destination = await getDestination(destinationId);
    return { steps, destination };
}

async function processEvent(event) {
    const results = await processEventBatch(event.pipelineId, [event]);
    return results[0];
}

function setupConsoleLogRedirection(vm, logs) {
    const logLevels = ['log', 'error', 'warn', 'info'];

    logLevels.forEach(level => {
        vm.on(`console.${level}`, (...args) => {
            try {
                const logAsString = args.map(arg => JSON.stringify(arg)).join(' ');
                logs.push(`${level.toUpperCase()}: ${logAsString}`);
            } catch (error) {
                logs.push(`LOG: ${error.message} (error while parsing log)`); //ignore
            }
        });
    });
}

async function processEventBatch(pipelineId, events) {
    const responses = [];
    try {
        const pipeline = await getPipeline(pipelineId);
        if (!pipeline) {
            throw "Pipeline not found";
        } else if (!pipeline.active) {
            throw "Pipeline is not active";
        }
        let logs = [];

        const vm = getVM();

        setupConsoleLogRedirection(vm, logs);

        const pipelineSteps = await resolveSteps(pipeline);

        const destination = pipelineSteps.destination;
        if (!destination || !destination.active) {
            logger.error("Invalid destination");
            throw "Destination is inactive or invalid";
        }
        const destinationWrappedObject = await getDestinationWrappedObject(vm, destination);

        if (destinationWrappedObject.setup && typeof destinationWrappedObject.setup === 'function') {
            await destinationWrappedObject.setup(destination.config);
        }

        for (const event of events) {
            const { id, payload } = event;
            let ctx = {}, evt = { ...payload }, consoleLogs;
            try {
                for (const step of pipelineSteps.steps) {
                    if (!step.active) {
                        continue;
                    }
                    evt = await runStep(vm, step, evt, ctx);
                    if (!evt) {
                        //transformations must return processed event for continuing with next steps
                        break;
                    }
                }

                if (evt && destinationWrappedObject.execute && typeof destinationWrappedObject.execute === 'function') {
                    await destinationWrappedObject.execute(evt, ctx, destination.config);
                }
                consoleLogs = logs.join('\n');
                responses.push({ id, success: true, consoleLogs });
            } catch (error) {
                if (typeof error === 'string') {
                    logs.push(`ERROR: ${error} (error while processing event)`);
                } else {
                    logs.push(`ERROR: ${error.message} (error while processing event)`);
                }
                consoleLogs = logs.join('\n').substring(0, 3999);
                responses.push({ id, success: false, consoleLogs });
            } finally {
                logs = [];
            }
        }

        if (destinationWrappedObject.teardown && typeof destinationWrappedObject.teardown === 'function') {
            await destinationWrappedObject.teardown(destination.config);
        }
    } catch (error) {
        if (responses.length === 0) {
            //if no events were processed, return error for all events
            for (const event of events) {
                responses.push({ id: event.id, success: false, consoleLogs: error.message });
            }
        }
        logger.error("Error while processing event", error, error.message);
    }
    return responses;
}
module.exports = {
    processEvent,
    processEventBatch
};