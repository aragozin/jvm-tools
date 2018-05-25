(function($, doc, wnd) {    
    
    function text(t) {
        return doc.createTextNode(t);
    }
    
    function createThreadFilterHandler(flameModel) {

        var suppressUpdate = false;
        var updater = function(){};
        var panel$ = null;

        function updateFilter() {            
            var nfilter = [];
            panel$.find("p input").each(function() {
                var info = $(this).data("thread");
                if ($(this).prop("checked")) {
                    nfilter.push(info.name);
                }                
            });
            debug("update thread filter: " + nfilter);
            if ("" + nfilter != "" + flameModel.filters.threads) {
                flameModel.filters.threads = nfilter;
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
                if (histo[x].count > 0) {
                    box.append(                    createStateItem(histo[x], up));
                }
            }   
            box.appendTo(panel$);
            makeToolbar();
        }
        
        function updateVisibility() {
            var filter = panel$.find(".search input").prop("value");
            debug("filter threads: " + filter);
            
            var vcnt = 0;
            panel$.find("p").each(function() {
                var p = $(this);
                p.find("input").each(function() {
                    var info = $(this).data("thread");    
                    if (filter.length == 0 || info.name.indexOf(filter) >= 0) {
                        vcnt++;
                        p.show();
                    }            
                    else {
                        p.hide();
                    }
                });
            });            
            
            if (vcnt == 0) {
                panel$.find("p.empty").show();
            }
            else {
                panel$.find("p.empty").hide();
            }
        }
        
        function all() {
            suppressUpdate = true;
            panel$.find("p>input").each(function() {
                if ($(this).is(":visible")) {
                    this.checked = true;
                }
            });
            suppressUpdate = false;
            updateFilter();
        }
        
        function none() {
            suppressUpdate = true;
            panel$.find("p>input").each(function() {
                if ($(this).is(":visible")) {
                    this.checked = false;
                }
            });
            suppressUpdate = false;            
            updateFilter();
        }
        
        function only() {
            suppressUpdate = true;
            panel$.find("p>input").each(function() {
                this.checked = $(this).is(":visible");
            });
            suppressUpdate = false;
            updateFilter();
        }
        
        function makeToolbar() {
            var div = $("<div class='search'/>");
            $("<input type='text''/>").keyup(updateVisibility).appendTo(div);
            $("<a class='all'/>").text("all").click(all).appendTo(div);
            $("<a class='only'/>").text("only").click(only).appendTo(div);
            $("<a class='none'/>").text("none").click(none).appendTo(div);
            panel$.prepend(div);
            
            $("<p class='empty''/>").text("no threads matching").hide().appendTo(panel$);
        }
        
        function createStateItem(stateInfo, updateFilter) {
            var p = $("<p></p>");
            var check = $("<input type='checkbox'></input>");
            var name = stateInfo.name;
            var caption = text(stateInfo.name);
            var checked = !flameModel.filters.threads || flameModel.filters.threads.indexOf(name) >= 0;
            check.attr("checked", checked);
            p.append(check);
            p.append(caption);
            check.change(function() {
                if (!suppressUpdate) {
                    updateFilter(); 
                }
            });
            check.data("thread", stateInfo);
            return p;
        }
        
        var handler = {
            
            histo: null,
            
            setUpdater: function(callback) {
                updater = callback;
            },
            
            isEnabled: function() {
                return flameModel.data.threads.length > 1;
            },

            updateCaption: function (btn$) {
                if (handler.isEnabled()) {
                    btn$.show();
                    var histo = flameModel.getThreadHistogram();
                    var tcount = 0;
                    for(var x in histo) {
                        tcount++;
                    }
                    if (flameModel.filters.threads) {
                        var ft = flameModel.filters.threads;
                        if (ft.length == tcount) {
                            btn$.text(tcount + " threads");    
                        }
                        else {
                            btn$.text(ft.length + " of " + tcount + " threads");    
                        }                        
                    }
                    else {
                        btn$.text(tcount + " threads");    
                    }
                }
                else {
                    btn$.hide();
                }
            },

            initContent: function (panel) {
                panel$ = panel;
                panel$.empty();
                makePanel(flameModel.getThreadHistogram());
            }
        };   
        
        return handler;
    }
    
    wnd.createThreadFilterHandler = createThreadFilterHandler;
    
}(jQuery, document, window));