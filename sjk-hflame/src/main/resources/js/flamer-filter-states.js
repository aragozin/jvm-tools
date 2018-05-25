(function($, doc, wnd) {    
    
    function text(t) {
        return doc.createTextNode(t);
    }
    
    function forEachArrayElement(array, func) {
        var len = array.length;
        for(var i = 0; i < len; ++i) {
            func(array[i]);
        }
    }    
    
    function createStateFilterHandler(flameModel) {

        var suppressUpdate = false;
        var updater = function(){};
        var panel$ = null;

        function stateCount(histo) {
            var cnt = 0;
            for(var x in histo) {
                if (histo[x].count > -1) {
                    ++cnt;
                }
            }
            return cnt;
        }

        function updateFilter() {            
            var nfilter = [];
            panel$.find("input").each(function() {
                var info = $(this).data("state");
                if ($(this).prop("checked")) {
                    nfilter.push(info.state);
                }                
            });
            debug("updating state filter: "  + nfilter);
            if ("" + nfilter != "" + flameModel.filters.states) {
                flameModel.filters.states = nfilter;
                if (flameModel.update) {
                    flameModel.update();
                }
            }          
            updater(); // notify visuals            
        }
        
        function makePanel(histo) {
            function up() {
                updateFilter(panel$);
            }
            var box = $("<div></div>");
            for(var x in histo) {
                if (histo[x].count > -1) {
                    box.append(                    createStateItem(histo[x], up));
                }
            }   
            box.appendTo(panel$);
        }
        
        function createStateItem(stateInfo, updateFilter) {
            var p = $("<p></p>");
            var check = $("<input type='checkbox'></input>");
            var lable = text(stateName(stateInfo.name) + " - " + stateInfo.count + " samples");
            var stcode = stateInfo.state;
            var checked = !flameModel.filters.states || flameModel.filters.states.indexOf(stcode) >= 0;
            check.attr("checked", checked);
            p.append(check);
            p.append(lable);
            check.change(function() {
                if (!suppressUpdate) {
                    updateFilter();                     
                }
            });
            check.data("state", stateInfo);
            return p;
        }
        
        function stateName(name) {
            if (name == "(???)") {
                return terminal;
            }
            else {
                return name.slice(1, name.length - 1);
            }
        }
        
        var handler = {
            
            histo: null,
            
            setUpdater: function(callback) {
                updater = callback;
            },
            
            isEnabled: function() {
                histo = flameModel.getStateHistogram();
                var enabled = stateCount(histo) > 1;
                debug("stateCount: " + stateCount(histo));
                return enabled;
            },

            updateCaption: function (btn$) {               
                if (handler.isEnabled()) {
                    btn$.show();
                    if (flameModel.filters.states) {
                        var fs = [];
                        for(var i = 0; i < flameModel.filters.states.length; ++i) {
                            var s = flameModel.filters.states [i];
                            fs.push(stateName(flameModel.data.frames[s]));
                        }
                        var stcount = stateCount(histo);
                        if (fs.length == 1) {
                            btn$.text(fs[0]);    
                        }
                        else if (fs.length == stcount) {
                            btn$.text("all states");
                        }
                        else {
                            btn$.text(fs.length + " of " + stcount + " states");    
                        }                        
                    }
                    else {
                        btn$.text("all states");
                    }
                }
                else {
                    btn$.hide();
                }
            },

            initContent: function (panel) {
                panel$ = panel;
                panel$.empty();
                makePanel(flameModel.getStateHistogram());
            }
        };   
        
        return handler;
    }
    
    wnd.createStateFilterHandler = createStateFilterHandler;
    
}(jQuery, document, window));