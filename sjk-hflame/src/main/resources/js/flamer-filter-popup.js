"use strict";

(function($, doc, wnd) {    
    
    function isInsideElement(mousee, node) {
        if (node.is(":visible")) {
            var x = mousee.pageX;
            var y = mousee.pageY;
            var l = node.offset().left;
            var t = node.offset().top;
            var w = node.outerWidth();
            var h = node.outerHeight();
            if (x >= l && x < l +w && y >= t && y < t + h) {
                return true;
            }
        }    
        return false;
    }     
    
    function initFilterPanel(div$, filterHandler) {
        
        var btn$ = div$.find(".btn");
        var panel$ = div$.find(".panel");
        
        btn$.click(function() {            
            if (panel$.is(":visible")) {
                debug("panel visible");
                hidePanel();
            }
            else {
                debug("panel hidden");
                showPanel();
            }
        });
        
        filterHandler.setUpdater(function() {
            filterHandler.updateCaption(btn$); 
        });
        filterHandler.updateCaption(btn$);
    
        $(wnd).click(function(e) {
            checkOutOfThreadFilterClick(e);             
        });
    
        function checkOutOfThreadFilterClick(e) {
            if (!isInsideElement(e, btn$)) {
                if (panel$.is(":visible")) {
                    if (!isInsideElement(e, panel$)) {
                        debug("click outside");
                        hidePanel();
                    }
                    else {
                        debug("click inside");
                    }
                }
            }
        }
        
        function showPanel() {
            debug("showPanle");
            if (filterHandler.isEnabled()) {
                panel$.empty();
                filterHandler.initContent(panel$);
                panel$.show();
            }            
        }
        
        function hidePanel() {
            debug("hidePanel");
            panel$.hide();
        }
    }
    
    wnd.initFilterPanel = initFilterPanel;
    
}(jQuery, document, window));

/* !!THROW AWAY BELOW!! */

var dummy_handler = {
    
    counter: 1,
    
    setUpdater: function(updater) {
        // do nothing
    },
    
    isEnabled: function() {
        return true;
    },

    updateCaption: function (btn$) {
        btn$.empty();
        btn$.text("dummy #" + dummy_handler.counter++);
    },

    initContent: function (panel$) {
        panel$.empty();
        panel$.text("dumm panel #" + dummy_handler.counter++);
    }
}