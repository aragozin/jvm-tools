"use strict"

function wrapFlameModel(data) {

    function forEachArrayElement(array, func) {
        var len = array.length;
        for(var i = 0; i < len; ++i) {
            func(array[i]);
        }
    }
    
    function isEmpty(filters) {
        return !(filters && (filters.threads || filters.states || filters.zoom));
    }

    function applyFilter(data, filter) {
        if (isEmpty(filter)) {
            return data;
        }
        else {
            var result = {};
            result.frames = data.frames;
            result.frameColors = data.frameColors;
            result.threads = [];

            var len = data.threads.length;
            for(var i = 0; i < len; ++i) {
                var thread = data.threads[i];
                if (!filter.threads || filter.threads.indexOf(thread.name) >= 0) {
                    var nthread = applyFilterToThread(thread, filter);
                    if (nthread.traces.length > 0) {
                        result.threads.push(nthread);
                    }
                }
            }

            return result;
        }
    }

    function applyFilterToThread(thread, filter) {
        if (filter.states || filter.zoom) {
            var nthread = {};
            nthread.name = thread.name;
            nthread.traces = [];

            var len = thread.traces.length;
            for(var i = 0; i < len; ++i) {
                var trace = thread.traces[i];
                var state = trace.trace[trace.trace.length - 1];
                if (!filter.states || filter.states.indexOf(state) >= 0) {
                    // pass state filter
                    var ntrace = {};
                    ntrace.trace = applyZoom(trace.trace, filter);
                    ntrace.samples = trace.samples;

                    if (ntrace.trace) {
                        nthread.traces.push(ntrace);
                    }
                }
            }
            return nthread;
        }
        else {
            return thread;   
        }
    }

    function applyZoom(trace, filter) {
        var i;
        if (filter.zoom) {
            if (filter.zoom[0] == -1) {
                // frame zoom
                var ntrace = trace;
                for(i = 1; i < filter.zoom.length; ++i) {
                    var frame = filter.zoom[i];
                    var n = ntrace.indexOf(frame);
                    if (n < 0) {
                        return null;
                    }
                    else {
                         ntrace = ntrace.slice(n);    
                    }
                }
                return ntrace;
            }
            else {
                // path zoom
                var len = filter.zoom.length;
                if (trace.length < len) {
                    return null;
                }
                for(i = 0; i < len; ++i) {
                    if (trace[i] != filter.zoom[i]) {
                        return null;
                    }
                }
                return trace.slice(len - 1);            
            }
        }
        else {
            return trace;
        }    
    }
    
    function forEachTrace(data, func) {
        forEachArrayElement(data.threads, function(thread) {
           forEachArrayElement(thread.traces, func); 
        });
    }
    
    function getStateHistogram(data, proto) {
        var result = {};
        for(var x in proto) {
            var info = proto[x];
            result[x] = {state: info.state, name: info.name, count: 0}
        }
        forEachTrace(data, function(trace) {
            var st = trace.trace[trace.trace.length - 1];
            var stName = data.frames[st];
            var stc = "" + st;
            if (result[stc]) {
                result[stc].count += trace.samples;
            }
            else {
                result[stc] = {state: st, name: stName, count: trace.samples};
            }
        });
        
        return result;
    }
    
    function getThreadHistogram(data) {
        var result = {};
        forEachArrayElement(data.threads, function(thread) {
            var count = 0;
            forEachArrayElement(thread.traces, function(trace) {
                count += trace.samples;                    
            });
            
            result[thread.name] = {name: thread.name, count: count};
        });
        
        return result;
    }

    // Method boy
    
    var flameModel = {};
    flameModel.data = data;
    flameModel.filters = {};
    
    flameModel.onModelUpdated = {};
    
    var fullStateHistogram = null;
    var stateHistogram = null;
    var threadHistogram = null;
    
    flameModel.update = function() {
        
        debug("updating flame model ...");
        
        var filters = flameModel.filters;
        var nozoom  = {
            threads: filters.threads,
            states: filters.states
        }
        
        flameModel.filteredData = applyFilter(flameModel.data, nozoom);
        
        flameModel.zoomedData = applyFilter(flameModel.filteredData, filters);
        
        stateHistogram = null;
        threadHistogram = null;
        
        for(var x in flameModel.onModelUpdated) {
            flameModel.onModelUpdated[x]();
        }
    };  
    
    flameModel.getStateHistogram = function() {
        if (!stateHistogram) {
            debug("calculate state histo");
            if (!fullStateHistogram) {
                fullStateHistogram = getStateHistogram(flameModel.data, {});
            }
            var filter = {threads: flameModel.filters.threads};
            stateHistogram = getStateHistogram(applyFilter(flameModel.data, filter), fullStateHistogram);
        }
        return stateHistogram;
    }

    flameModel.getThreadHistogram = function() {
        if (!threadHistogram) {
            debug("calculate thread histo");
            var filter = {states: flameModel.filters.states};
            threadHistogram = getThreadHistogram(applyFilter(flameModel.data, filter));
        }
        return threadHistogram;
    }
    
    return flameModel;
}

/* !!THROW AWAY BELOW!! */

/*
var flameModel = {
    data: {
        frames: [ "" ],
        frameColors: [ "" ],
        threads: [
            {
                name: "Main",
                traces: [
                    { trace: [1, 2, 4], samples: 100 },
                ]
            }
        ]
    },
    filters: {
        threads: [""],
        states: [1, 2, 3],
        zoom: [-1, 1],
    },
    zoomPath: [1, 2, 3]
}            
*/

