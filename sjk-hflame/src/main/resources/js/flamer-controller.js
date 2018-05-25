"use strict";

function createFlameChart(hostId, data) {

    var $ = window.$; // I'm lazy to configure linting
    var wnd = window;
    var doc = document;
    
    var box$ = $("#" + hostId);
    
    
    var flameChart = {
        
        initFlameChart: function() {
            
            var wmodel = wrapFlameModel(data);
            wmodel.update();
    
            initFilterPanel($("#" + hostId + " div.flameStateFilter"), createStateFilterHandler(wmodel));
    
            initFilterPanel($("#" + hostId + " div.flameThreadFilter"), createThreadFilterHandler(wmodel));
    
            initFlameGraph(box$, wmodel);
            
            // for debuging
            wnd.flameModel = wmodel;
        }        
    };
    
    return flameChart;
}

