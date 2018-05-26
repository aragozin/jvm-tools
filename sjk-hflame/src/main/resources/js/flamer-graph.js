(function($, doc, wnd) {    

    function sampleCount(dataSet, prefix) {

        if (prefix === undefined) {
            prefix = [];
        }

        var count = 0;

        for(var i = 0; i < dataSet.threads.length; ++i) {
            var td = dataSet.threads[i];
            for(var j = 0; j < td.traces.length; ++j) {
                if (arrayStartsWith(td.traces[j].trace, prefix)) {
                    count += td.traces[j].samples;
                }
            }
        }

        return count;
    }

    function sampleCountForFrame(dataSet, frame) {

        var count = 0;

        for(var i = 0; i < dataSet.threads.length; ++i) {
            var td = dataSet.threads[i];
            for(var j = 0; j < td.traces.length; ++j) {
                if (td.traces[j].trace.indexOf(frame) >= 0) {
                    count += td.traces[j].samples;
                }
            }
        }

        return count;
    }

        function arrayStartsWith(a, pref) {
        if (a.length < pref.length) {
            return false;
        }
        for(var i = 0; i < pref.length; ++i) {
            if (a[i] != pref[i]) {
                return false;
            }
        }
        return true;
    }

    function toPath(a) {
        var p = "p";
        for(var i = 0; i < a.length; ++i) {
            if (p != "p") {
                p += "_";
            }
            p += a[i];
        }
        return p;
    }

    function toPrefix(a) {
        var f = a.lastIndexOf("_");
        a = a.slice(0, f);
        a = a.slice(a.lastIndexOf("p") + 1);
        var pref = a.split("_");
        for(var i = 0; i < pref.length; ++i) {
            pref[i] = Number(pref[i]);
        }
        return pref;
    }

    function selectByPath(tree, path) {
        var node = tree;
        for(var i = 0; i < path.length; ++i) {
            node = node["f" + path[i]];
            if (node === undefined) {
                return node;
            }
        }
        return node;
    }

    function collectTree(dataSet) {
        var root = {};

        var i;
        for(i = 0; i < dataSet.threads.length; ++i) {
            var j;
            var td = dataSet.threads[i];
            for(j = 0; j < td.traces.length; ++j) {
                var k;
                var t = td.traces[j].trace;
                var node = root;
                for(k = 0; k < t.length; ++k) {
                    var f = "f" +  t[k];
                    if (node[f] === undefined) {
                        node[f] = {};
                    }
                    node = node[f];
                    var pref = t.slice(0, k + 1);
                    node.path = toPath(pref);
                    node.frame = dataSet.frames[t[k]];
                    node.frameNo = t[k];
                    node.samples = sampleCount(dataSet, pref);
                }
            }
        }

        root.samples = sampleCount(dataSet, []);

        return root;
    }
    
    function createInfoElement(ns, treeNode) {
        if (treeNode.frame === undefined) {
            var stub = $("<div/>");
            stub.css({display: "none"});
            stub.text("no frame");
            return stub;        
        }
        else if (treeNode.frame == "(WAITING)") {
            var wnode = $("<div class='waitSmoke flameNode'/>");
            wnode.attr("id", ns + treeNode.path + "_node");
            return wnode;
        }
        else if (treeNode.frame == "(TIMED_WAITING)") {
            var twnode = $("<div class='twaitSmoke flameNode'/>");
            twnode.attr("id", ns + treeNode.path + "_node");
            return twnode;
        }
        else if (treeNode.frame == "(BLOCKED)") {
            var bnode = $("<div class='blockSmoke flameNode'/>");
            bnode.attr("id", ns + treeNode.path + "_node");
            return bnode;        
        }
        else if (treeNode.frame == "(RUNNABLE)") {
            var rnode = $("<div class='hotSmoke flameNode'/>");
            rnode.attr("id", ns + treeNode.path + "_node");
            return rnode;        
        }
        else if (treeNode.frame == "(IO)") {
            var ionode = $("<div class='ioSmoke flameNode'/>");
            ionode.attr("id", ns + treeNode.path + "_node");
            return ionode;        
        }
        else if (treeNode.frame == "(???)") {
            var tnode = $("<div class='termSmoke flameNode'/>");
            tnode.attr("id", ns + treeNode.path + "_node");
            return tnode;        
        }
        else {
            var fnode = $("<div class='execNode flameNode'/>");
            fnode.addClass(ns + "fr" + treeNode.frameNo)
            fnode.attr("id", ns + treeNode.path + "_node");
            fnode.text(treeNode.frame);
            return fnode;        
        }
    }
    
    function createTreeElement(ns, treeNode, weight, threshold) {
        if (treeNode.samples < threshold) {
            var stub = $("<div/>");
            stub.css({display: "none"});
            stub.text("small element stub");
            return stub;
        }
        else {
            var div = $("<div class='flameBox'/>");
            if (treeNode.path !== undefined) {
                div.attr("id", ns + treeNode.path + "_box");            
            }
            div.css({flexBasis: (weight + "%")});
            var children = [];
            for(var prop in treeNode) {
                if (prop.startsWith("f")) {
                    children[children.length] = treeNode[prop];
                }
            }

            if (children.length == 1 && children[0].samples == treeNode.samples) {
                div.append(createTreeElement(ns, children[0], 100, threshold));
            }
            else if (children.length > 0) {
                children.sort(function(a, b) {a.samples - b.samples});
                var row = $("<div class='flameRow'/>");
                for(var i = 0; i < children.length; ++i) {
                    var cw = 100 * children[i].samples / treeNode.samples;
                    var node = createTreeElement(ns, children[i], cw, threshold);
                    row.append(node);
                }
                div.append(row);
            }

            var finfo = createInfoElement(ns, treeNode);
            div.append(finfo);
            return div;
        }    
    }    
    
    function placeHover(container, hover, event) {
        var hoverx = event.pageX + 10;
        var hovery = event.pageY + 15;
        var hw = hover.width();
        var hh = hover.height();
        var cx = container.position().left;
        var cw = container.outerWidth();
        var cy = container.position().top;
        var ch = container.outerHeight();

        if (hoverx + hw > cx + cw) {
            if (hw > cw) {
                hoverx = cx;
            }
            else {
                hoverx = cx + cw - hw;
            }
        }
        
        if (hovery + hh > cy + ch) {
            hovery = event.pageY - hh -15; 
        }

        hover.css({ top: hovery, left: hoverx });
    }
    
    function fmtPercent(val) {
        return Number(val * 100).toFixed(2) + "%";
    }
    
    function toState(frame) {
        if (frame == "(???)") {
            return "Terminal";
        }
        else {
            return frame.slice(1, frame.length - 1);
        }
    }

    function createFrameGraph(host$, flameModel) {

        var hostId = host$.attr("id");

        function updateHoverText(node, prefix, tree, dataSet) {
            node.empty();
            debug("updateHoverText: " + prefix);
            var fid = prefix[prefix.length - 1];
            var fnode = selectByPath(tree, prefix);
            var frame = fnode.frame;
            if (frame.startsWith("(")) {
                // this is terminator frame, display last frame
                var state = toState(frame);
                var stCount = fnode.samples;
                fid = prefix[prefix.length - 2];
                fnode = selectByPath(tree, prefix.slice(0, -1));
                frame = fnode.frame;
            }
            var totalSamples = sampleCount(dataSet, []);
            var nodeSampleCount = fnode.samples;
            var globalCount = sampleCountForFrame(dataSet, fid);
            $('<p class="hoverFrame"/>').text(frame).appendTo(node);
            if (state !== undefined) {
                var lbl = state + ": " + stCount + " (" + fmtPercent(stCount / totalSamples) + ")";
                $('<p class="hoverStats"/>').text(lbl).appendTo(node);   
            }
            $('<p class="hoverStats"/>').text("Sample count: " + nodeSampleCount + " (" + fmtPercent(nodeSampleCount / totalSamples) + ")").appendTo(node);
            $('<p class="hoverStats"/>').text("Global frame frequency: " + globalCount + " (" + fmtPercent(globalCount / totalSamples) + ")").appendTo(node);
            if (flameModel.filters.zoom) {
                var unzoomedCount = sampleCountForFrame(flameModel.filteredData, fid);
                $('<p class="hoverStats"/>').text("Unzoomed frame count: " + unzoomedCount).appendTo(node); 
            }            
        }                
        
        function zoomFrame(frame) {
            if (flameModel.filters.zoom) {
                flameModel.filters.zoom.push(frame);
            }
            else {
                flameModel.filters.zoom = [-1, frame];
            }
            debug("New zoom path: " + flameModel.filters.zoom);
            flameModel.update();
        }
        
        function zoomPath(path) {
            flameModel.filters.zoom = path;
            debug("New zoom path: " + path);
            flameModel.update();
        }
        
        function unzoom() {
            flameModel.filters.zoom = null;
            flameModel.update();
        }

        function createStackTrace(prefix, tree, dataSet) {
            var box$ = $("<div class='trace'/>");
            var i, fname;
            var ff = false;
            if (flameModel.filters.zoom) {
                var zoom = flameModel.filters.zoom;
                if (zoom[0] < 0) {
                    for(i = 1; i < zoom.length; ++i) {
                        fname = dataSet.frames[zoom[i]];
                        $("<p class='ff'/>").text(fname).prependTo(box$);
                    }
                    ff = true;
                }
                else {
                    for(i = 0; i < zoom.length - 1; ++i) {
                        fname = dataSet.frames[zoom[i]];
                        $("<p/>").text(fname).prependTo(box$);
                    }                    
                }
            }
            for(i = ff ? 1 : 0; i < prefix.length; ++i) {
                fname = dataSet.frames[prefix[i]];
                $("<p/>").text(fname).prependTo(box$);
            }        
            return box$;
        }
        
        function updatePopupText(node, prefix, tree, dataSet) {
            updateHoverText(node, prefix, tree, dataSet);
            debug("updatePopupText: " + prefix);
            createStackTrace(prefix, tree, dataSet).appendTo(node);
            var buttons = $("<div class='toolbar'/>");
            buttons.appendTo(node);
            $("<span/>").text("zoom by").appendTo(buttons);
            
            var zoom = flameModel.filters.zoom;
            
            if (!zoom || zoom[0] < 0) {
                var zoomByFrame = function() {
                    zoomFrame(prefix[prefix.length - 1]);
                }
            
                $("<a/>").text("frame").click(zoomByFrame).appendTo(buttons);
            }            
            if (!zoom || zoom[0] >= 0) {
                var path = (zoom ? zoom.slice(0, -1) : []).concat(prefix);
                var zoomByPath = function() {
                    zoomPath(path);
                }                
                $("<a/>").text("trace").click(zoomByPath).appendTo(buttons);
            }
        }
        
        function updateFramePallete(dataSet) {
            var pal = "";
            for(var i = 0; i < dataSet.frameColors.length; ++i) {
                if (dataSet.frameColors[i] != null && dataSet.frameColors[i] !== undefined) {
                    pal += "div." + hostId + "_fr" + i + " {background-color: " + dataSet.frameColors[i] + ";}\n";               
                }
            }
            var id = hostId + "_frameColors";
        
            updateStyleSection(id, pal);
        }
    
        function updateStyleSection(id, styleSheet) {
            var stBlock = $("<style id='" + id + "'></style>");
            stBlock.text(styleSheet);

            if ($("#" + id).length == 0) {
                $("html>head").append(stBlock);
            }
            else {
                $("#" + id).replaceWith(stBlock);
            }        
        }
        
        function installTooltips(tree, dataSet) {
            host$.unbind("mouseleave");
            host$.unbind("mousemove");
            host$.unbind("click");

            var lastHightlight = null;
            var lastTooltip = "";
            var pinTooltip = false;

            var hover$ = $("#" + hostId + ">div.flameHover");
            
            hover$.hide();
            
            updateStyleSection(hostId + "_highlight", "");

            $("#" + hostId + " .flameNode").mouseleave(function(){
                if (!pinTooltip) {
                    $("#" + hostId + ">div.flameHover").hide();
                }
            });

            $("#" + hostId + " .flameNode").mousemove(
                function(e) {
                    if (!pinTooltip) {
                        var node = this;
                        if (node == null) {
                            hover$.hide();
                            lastTooltip = "";
                        }
                        else {
                            hover$.show();
                            if (lastTooltip !=  node.id) {
                                lastTooltip = node.id;

                                updateHoverText(hover$, toPrefix(node.id), tree, dataSet);
                            }
                        }            
                        placeHover($("#" + hostId), hover$, e);
                    }
                }
            );
            
            function clickHandler(e) {
                e.stopImmediatePropagation();
                var highlight = null;
                var path = null;
                var node = this;
                var id = node.id;
                if (id && id.indexOf("_node")>=0) {
                    path = toPrefix(id);                       
                    node = selectByPath(tree, path);
                    debug("click on " + id);
                    if (node.frame.startsWith("(")) {
                        // status node not clickable
                    }
                    else {
                        debug("click id: " + path);
                        highlight = node.frameNo;
                    }
                }
                
                var showPopup = false;
                
                if (highlight && highlight == lastHightlight) {
                    if (hover$.is(":visible")) {
                        highlight = null;
                    }
                    else {
                        showPopup = true;
                    }
                }

                lastHightlight = highlight;
                
                if (highlight) {
                    debug("highlight: " + highlight);
                    var col = dataSet.frameColors[highlight];
                    var stl = ""
                    stl += "div." + hostId + "_fr" + highlight + " {";
                    stl += "border-color: " + col + ";";
                    stl += "background-color: #FAF;";
                    stl += "font-weight: bold;";
                    stl += "}\n";
                    
                    updateStyleSection(hostId + "_highlight", stl);
                    pinTooltip = true;
                    lastTooltip = "";
                                        
                    if (showPopup) {
                        updatePopupText(hover$, path, tree, dataSet);
                    
                        hover$.show().queue(function() {
                            placeHover($("#" + hostId), hover$, e);
                            hover$.clearQueue();
                        });
                    }
                    else {
                        hover$.hide();
                    }
                }
                else {
                    debug("highlight disable");
                    updateStyleSection(hostId + "_highlight", "");
                    pinTooltip = false;
                    hover$.hide();
                }
            }
            
            $("#" + hostId + " .flameNode").click(clickHandler);
            $("#" + hostId + " .flameArea").click(clickHandler);
        }

        function createZoomFrameNode(frame, frameNo) {
            var fnode = $("<div class='zoomFrame'/>");
            fnode.addClass(hostId + "_fr" + frameNo);
            fnode.text(frame);
            return fnode;                    
        }
        
        function createZoomBar() {
            var stack = $("<div class='zoomStack'/>");
            var bar = $("<div class='zoomBar'/>");
            var zoomInfo, i, fr, frame;
            if (flameModel.filters.zoom[0] < 0) {
                var n = flameModel.filters.zoom.length - 1;
                if (n > 1) {
                    zoomInfo = "zoom by " + n + " frames";
                }
                else {
                    zoomInfo = "zoom by frame";
                }                
                for(i = 0; i < n; ++i) {
                    fr = flameModel.filters.zoom[i + 1];
                    frame = flameModel.data.frames[fr];
                    createZoomFrameNode(frame, fr).prependTo(stack);
                }
            }
            else {
                zoomInfo = "zoom by trace";
                for(i = 0; i < flameModel.filters.zoom.length - 1; ++i) {
                    fr = flameModel.filters.zoom[i];
                    frame = flameModel.data.frames[fr];
                    createZoomFrameNode(frame, fr).prependTo(stack);
                }
            }
            
            $("<p class='zoomInfo'/>").text(zoomInfo).appendTo(bar);
            $("<p class='doUnzoom'/>").text("Click to unzoom").appendTo(bar);
            bar.click(unzoom);
            bar.prependTo(stack);
            return stack;
        }
        
        function redrawGraph() {

            var dataSet = flameModel.zoomedData;
            var rootNode = $(host$.find("div.flameRoot"));
            
            if (!dataSet.threads || dataSet.threads.length == 0) {
                rootNode.empty();
                $("<div class='empty'/>").text("No data matching").appendTo(rootNode);
            }
            else {
        
                var ns = hostId + "_";
                var totalSamples = sampleCount(dataSet, []);
                var graphWidth = rootNode.innerWidth();
                var tree = collectTree(dataSet);
                var threshold = 4 * totalSamples / graphWidth;
                var graph = createTreeElement(ns, tree, 100, threshold);

                rootNode.empty();
                rootNode.append(graph);
                
                if (flameModel.filters.zoom) {
                    debug("show zoom bar");
                    rootNode.append(createZoomBar());
                }

                installTooltips(tree, dataSet);
            }
        }
        
        // main
        
        updateFramePallete(flameModel.data);

        flameModel.onModelUpdated.redrawGraph = redrawGraph;
        
        redrawGraph();
    }
    
    wnd.initFlameGraph = createFrameGraph;
    
}(jQuery, document, window));