import { jsPlumb } from "jsplumb";





/**
 * Flow.regisGetUsers(()=>Array<{id: string, name: string}>)
 * Flow.drawWorkflow(json)
 * Flow.getDefinition(): json
 * Flow.setCurrentNode(nodeId: long)
 */
const Flow = ((function (nodeDom) {
    let utils = {
        selectMultip: ((function () {
            let selectMultip = {
                register: function (elem) {
                    let height = '17px';
                    elem.style.cssText = `
                    position:relative; 
                    height: ${height};
                    cursor: pointer;
                    margin: 0em;
                    font: 400 13.3333px Arial;
                    padding: 1px 2px;
                    border: 1px solid black;
                    border-radius: 2px;
                    `

                    let selectdContainer = document.createElement('div');
                    selectdContainer.style.cssText = `
                    display: flex;
                    width: 100%;
                    height: 100%;
                    `
                    elem.appendChild(selectdContainer);

                    function renderSelected() {
                        let c = '';
                        elem.value.forEach((v, index) => {
                            if (index !== elem.value.length - 1) {
                                c += `<span style="display: inline-block; margin: 0 2px;">${elem.__value_map.get(v)},</span>`
                            } else {
                                c += `<span style="display: inline-block; margin: 0 2px;">${elem.__value_map.get(v)}</span>`
                            }
                        });
                        selectdContainer.innerHTML = c;
                    }

                    let ul = elem.children[0];

                    ul.makeVisible = () => ul.style.cssText = `
                    position:absolute; 
                    top: calc(${height} + 5px); 
                    margin: 0;
                    background: white;
                    min-width: 100px;
                    border: 1px solid;
                    box-shadow: 0 2px 12px 0 black;
                    border-radius: 3px;
                    z-index: 999;
                    `;
                    ul.makeDispear = () => ul.style.cssText = `display: none`;
                    ul.makeDispear();

                    document.addEventListener("click", () => {
                        let event = window.event || arguments.callee.caller.arguments[0];
                        if (event.path.indexOf(selectdContainer) < 0) {
                            ul.makeDispear();
                            elem.__open = true;
                        }
                    })

                    elem.__open = true;
                    elem.onclick = () => {
                        elem.__open ? ul.makeVisible() : ul.makeDispear();
                        elem.__open = !elem.__open;
                    }

                    elem.value = [];
                    elem.__value_map = new Map();
                    for (let i = 0; i < ul.children.length; i++) {
                        let li = ul.children[i];
                        let livalue = li.getAttribute('value');
                        elem.__value_map.set(livalue, li.innerHTML);

                        if (li.hasAttribute("selected")) {
                            elem.value.push(livalue);
                        }

                        li.onclick = () => {
                            let event = window.event || arguments.callee.caller.arguments[0];
                            event.stopPropagation();
                            if (elem.value.indexOf(livalue) >= 0) {
                                elem.value = elem.value.filter(v => v !== livalue);
                            } else {
                                elem.value.push(livalue);
                            }
                            renderSelected();
                            if (elem.onchange) {
                                elem.onchange(elem.value);
                            }
                        }
                    }
                    renderSelected();
                },
            }
            return selectMultip;
        })()),
    }

    function genId() {
        var s = [];
        var hexDigits = "0123456789";
        for (var i = 0; i < 36; i++) {
            s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
        }
        s[14] = "4";  // bits 12-15 of the time_hi_and_version field to 0010
        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);  // bits 6-7 of the clock_seq_hi_and_reserved to 01
        s[8] = s[13] = s[18] = s[23] = "";
     
        var uuid = s.join("");
        return parseInt(uuid);
    }

    let getUsers = () => {
        return [];
    }

    let jsplumbisready = false;
    function waitReady(call) {
        let start = () => {
            if (jsplumbisready) {
                call();
            } else {
                setTimeout(() => {
                    start();
                }, 200)
            }
        }
        start();
    }

    function download(filename, text) {
        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);
    }

    let NodeType = {
        AUDIT: "AUDIT",
        EXCLUDE_GATEWAY: "EXCLUDE_GATEWAY",
        START: "START",
        END: "END",
    }

    /** @param {HTMLElement} container 
     * 
     * configs： {editable: boolean, extendMenu: Array<{name: string, onclick: ()=>void}>}
    */
    let Flow = function (container, configs) {
        let that = this;

        if (typeof container === 'string') {
            container = document.querySelector(container);
        }

        configs = configs || {}
        configs.editable = configs.editable || true;
        configs.extendMenu = configs.extendMenu || [];

        let btnStyle = `cursor: pointer;`
        container.innerHTML = `
            <div oId="eleSetter" style="display:none; position: absolute;"></div>
            <div style="display: flex; width: 100%; height: 100%;">
                <div oId="menu" style="width: 200px; border-right: 1px solid black">
                    <div style="display: flex; flex-direction: column; width: 100%; height: 100%">
                        <div style="width: 100%; flex-grow: 1;">
                            <div oId="startBtn" style="${btnStyle}">开始</div>
                            <div oId="endBtn" style="${btnStyle}">结束</div>
                            <div oId="auditBtn" style="${btnStyle}">审核</div>
                            <div oId="excludeBtn" style="${btnStyle}">排他网关</div>
                        </div>
                        <div style="height: 300px; width: 100%;">
                            <div oId="export" style="${btnStyle}">导出模型</div>
                            <div oId="import" style="${btnStyle}">导入模型</div>
                            ${configs.extendMenu.map(it => `<div oId="${it.name}" style="${btnStyle}">${it.name}</div>`).join('')}
                        </div>
                    </div>
                </div>
                <div oId="drawPane" style="flex-grow: 1; position: relative;"></div>
            </div>
        `

        configs.extendMenu.forEach(it => {
            container.querySelector(`div[oId="${it.name}"]`).onclick = it.onclick;
        });

        // 绘图板
        let drawPane = container.querySelector('div[oId="drawPane"]');
        // 元素属性的配置框
        let eleSetter = container.querySelector("div[oId='eleSetter']");
        eleSetter.makeVisible = () => eleSetter.style.cssText = 'position: absolute; box-shadow:0 2px 12px 0 black; z-index: 9999; background: white; right: 10px; top: 10px;';
        eleSetter.makeDispear = () => eleSetter.style.cssText = 'display:none; position: absolute;';

        // 装填各类点击事件
        if (configs.editable) {

            container.querySelector('div[oId="import"]').onclick = () => {
                const input = document.createElement('input');
                document.body.appendChild(input);
                input.style.cssText = "display: none;"
                input.setAttribute("type", "file");
                input.click();
                input.onchange = () => {
                    const file = input.files[0];
                    const reader = new FileReader();
                    reader.readAsText(file);
                    reader.onload = () => {
                        that.drawWorkflow(JSON.parse(reader.result));
                        document.body.removeChild(input);
                    }
                }
            }

            container.querySelector('div[oId="export"]').onclick = () => {
                download('model', JSON.stringify(that.getDefinition(), null, 4));
            }

            container.querySelector('div[oId="auditBtn"]').onclick = () => {
                genNode(NodeType.AUDIT);
            }

            container.querySelector('div[oId="excludeBtn"]').onclick = () => {
                genNode(NodeType.EXCLUDE_GATEWAY);
            }

            container.querySelector('div[oId="startBtn"]').onclick = () => {
                genNode(NodeType.START);
            }

            container.querySelector('div[oId="endBtn"]').onclick = () => {
                genNode(NodeType.END);
            }
        } else {
            let menu = container.querySelector('div[oId="menu"]');
            menu.innerHTML = '';
            menu.style.cssText = '';
        }

        function genNode(nodeType, option) {
            let div = null;

            switch (nodeType) {
                case NodeType.EXCLUDE_GATEWAY: {
                    option = option || {
                        offsetLeft: 30,
                        offsetTop: 30,
                        name: '',
                        updot: 'allow',
                        downdot: 'reject',
                        id: genId(),
                    };
                    div = document.createElement('div');
                    div.style.cssText = `
                        display: flex; 
                        width: 100px; 
                        height: 100px; 
                        position: absolute;
                        align-items: center;
                        left: ${option.offsetLeft}px;
                        top: ${option.offsetTop}px;
                        justify-content: center;
                    `;
                    drawPane.appendChild(div);
                    div.__flow_type = NodeType.EXCLUDE_GATEWAY;
                    div.__flow_data = div.__flow_data || {
                        updot: option.updot,
                        downdot: option.downdot,
                        name: option.name,
                        id: option.id,
                    };
                    div.innerHTML = `<div 
                    style="
                    background:#f5f5dc;
                    width: 70px;
                    height: 70px;
                    transform: rotate(45deg);
                    box-shadow:0 2px 12px 0 #00d6fd;
                    "
                    ></div><div style="position: absolute">${div.__flow_data.name}</div>`
                    that.instance.addEndpoint(div, {
                        isTarget: true,
                        anchor: "Left",
                        endpointStyle: { fill: "#fd4700" },
                        maxConnections: Number.MAX_SAFE_INTEGER,
                    });

                    let upendpoint = that.instance.addEndpoint(div, {
                        isSource: true,
                        anchor: "Top",
                        connectorOverlays: [
                            ["Arrow", { width: 10, length: 30, location: 1, id: "arrow" }],
                            ["Label", { label: div.__flow_data.updot === 'allow' ? "Y" : "N", location: 0.1, id: "label" }],
                        ],
                    });

                    let downendpoint = that.instance.addEndpoint(div, {
                        isSource: true,
                        anchor: "Bottom",
                        connectorOverlays: [
                            ["Arrow", { width: 10, length: 30, location: 1, id: "arrow" }],
                            ["Label", { label: div.__flow_data.downdot === 'reject' ? "N" : "Y", location: 0.1, id: "label" }],
                        ],
                    });

                    if (configs.editable) {
                        div.addEventListener("dblclick", () => {
                            eleSetter.makeVisible();
                            eleSetter.innerHTML = `
                                <table>
                                    <tr><td>流程节点名: </td><td><input oId="name" value="${div.__flow_data.name}"></td></tr>
                                    <tr><td>上节点类型: </td><td>allow<input oId="upallow" type="checkbox">reject<input oId="upreject" type="checkbox"></td></tr>
                                    <tr><td>下节点类型: </td><td>allow<input oId="downallow" type="checkbox">reject<input oId="downreject" type="checkbox"></td></tr>
                                </table>
                                <div><span style="float: right; display: inline-block;"><button oId="deleteNode">删除节点</button><button oId="ok">确认</button></span></div>
                            `
                            eleSetter.querySelector('input[oId="name"]').onblur = (evt) => {
                                div.__flow_data.name = evt.target.value;
                                div.children[1].innerHTML = evt.target.value;
                            }
                            eleSetter.querySelector('button[oId="deleteNode"]').onclick = (evt) => {
                                that.instance.remove(div);
                                eleSetter.makeDispear();
                            }
                            eleSetter.querySelector('button[oId="ok"]').onclick = (evt) => {
                                eleSetter.makeDispear();
                            }

                            function setChecked(upallow, upreject, downallow, downreject) {
                                let upconn = null;
                                let downconn = null;

                                that.instance.select().each((conn) => {
                                    if (conn.source === div) {
                                        if (conn.endpoints[0].anchor.type === 'Bottom') {
                                            downconn = conn;
                                        } else {
                                            upconn = conn;
                                        }
                                    }
                                });

                                if (upallow) {
                                    div.__flow_data.updot = 'allow';
                                    div.__flow_data.downdot = 'reject';
                                    upendpoint.connectorOverlays[1][1].label = "Y"
                                    downendpoint.connectorOverlays[1][1].label = "N"
                                    if (upconn) {
                                        upconn.getOverlay("label").setLabel("Y");
                                    }
                                    if (downconn) {
                                        downconn.getOverlay("label").setLabel("N");
                                    }
                                } else {
                                    div.__flow_data.updot = 'reject';
                                    div.__flow_data.downdot = 'allow';
                                    upendpoint.connectorOverlays[1][1].label = "N"
                                    downendpoint.connectorOverlays[1][1].label = "Y"
                                    if (upconn) {
                                        upconn.getOverlay("label").setLabel("N");
                                    }
                                    if (downconn) {
                                        downconn.getOverlay("label").setLabel("Y");
                                    }
                                }
                                eleSetter.querySelector('input[oId="upallow"]').checked = upallow;
                                eleSetter.querySelector('input[oId="upreject"]').checked = upreject;
                                eleSetter.querySelector('input[oId="downallow"]').checked = downallow;
                                eleSetter.querySelector('input[oId="downreject"]').checked = downreject;
                            }

                            let initial = div.__flow_data.updot === 'allow';
                            setChecked(initial, !initial, !initial, initial);
                            eleSetter.querySelector('input[oId="upallow"]').onclick = (evt) => {
                                if (evt.target.checked) {
                                    setChecked(true, false, false, true);
                                } else {
                                    setChecked(false, true, true, false);
                                }
                            }
                            eleSetter.querySelector('input[oId="upreject"]').onclick = (evt) => {
                                if (evt.target.checked) {
                                    setChecked(false, true, true, false);
                                } else {
                                    setChecked(true, false, false, true);
                                }
                            }
                            eleSetter.querySelector('input[oId="downallow"]').onclick = (evt) => {
                                if (evt.target.checked) {
                                    setChecked(false, true, true, false);
                                } else {
                                    setChecked(true, false, false, true);
                                }
                            }
                            eleSetter.querySelector('input[oId="downreject"]').onclick = (evt) => {
                                if (evt.target.checked) {
                                    setChecked(true, false, false, true);
                                } else {
                                    setChecked(false, true, true, false);
                                }
                            }
                        });
                    }

                    break;
                }
                case NodeType.AUDIT: {
                    option = option || {
                        offsetLeft: 30,
                        offsetTop: 30,
                        name: '',
                        id: genId(),
                        auditor: [],
                    };
                    div = document.createElement('div');
                    div.style.cssText = `
                    background:#f5f5dc; 
                    height: 100px; 
                    width: 147px; 
                    position: absolute;
                    box-shadow:0 2px 12px 0 #00d6fd;
                    border-radius: 17px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    left: ${option.offsetLeft}px;
                    top: ${option.offsetTop}px;
                    `;
                    drawPane.appendChild(div);
                    div.__flow_type = NodeType.AUDIT;
                    div.__flow_data = {
                        id: option.id,
                        name: option.name,
                        auditor: option.auditor,
                    };
                    div.innerHTML = `<div>${div.__flow_data.name}</div>`
                    that.instance.addEndpoint(div, {
                        isSource: true,
                        anchor: 'Right',
                        connectorOverlays: [
                            ["Arrow", { width: 10, length: 30, location: 1, id: "arrow" }],
                        ],
                    });
                    that.instance.addEndpoint(div, {
                        isTarget: true,
                        anchor: 'Left',
                        endpointStyle: { fill: "#fd4700" },
                        maxConnections: Number.MAX_SAFE_INTEGER,
                    });
                    if (configs.editable) {
                        div.addEventListener("dblclick", () => {
                            eleSetter.makeVisible();
                            eleSetter.innerHTML = `
                                <table>
                                    <tr><td>流程节点名: </td><td><input oId="name" value="${div.__flow_data.name}"></td></tr>
                                    <tr><td>审核人: </td><td>
                                        <div oId="selectorUser">
                                            <ul>
                                                ${getUsers().map(v => `<li value="${v.id}" ${div.__flow_data.auditor.indexOf(v.id) > -1 ? 'selected' : ''}>${v.name}</li>`).join('')}
                                            </ul>
                                        </div>
                                    </td></tr>
                                </table>
                                <div><span style="float: right; display: inline-block;"><button oId="deleteNode">删除节点</button><button oId="ok">确认</button></span></div>
                            `;
                            let selectorUser = eleSetter.querySelector('div[oId="selectorUser"]');
                            utils.selectMultip.register(selectorUser);

                            selectorUser.onchange = (value) => {
                                div.__flow_data.auditor = value;
                            }


                            eleSetter.querySelector('input[oId="name"]').onblur = (evt) => {
                                div.__flow_data.name = evt.target.value;
                                div.children[0].innerHTML = evt.target.value;
                            }
                            eleSetter.querySelector('button[oId="deleteNode"]').onclick = (evt) => {
                                that.instance.remove(div);
                                eleSetter.makeDispear();
                            }
                            eleSetter.querySelector('button[oId="ok"]').onclick = (evt) => {
                                eleSetter.makeDispear();
                            }
                        });
                    }
                    break;
                }
                case NodeType.START: {
                    option = option || {
                        offsetLeft: 30,
                        offsetTop: 30,
                        id: genId(),
                    };
                    div = document.createElement('div');
                    div.style.cssText = `
                    background:#f5f5dc; 
                    height: 100px; 
                    width: 100px; 
                    position: absolute;
                    box-shadow:0 2px 12px 0 #00d6fd;
                    border-radius:100px;
                    left: ${option.offsetLeft}px;
                    top: ${option.offsetTop}px;
                    `
                    drawPane.appendChild(div);
                    div.__flow_type = NodeType.START;
                    div.__flow_data = {
                        id: option.id,
                        name: 'start',
                    };
                    that.instance.addEndpoint(div, {
                        isSource: true,
                        anchor: 'Right',
                        connectorOverlays: [
                            ["Arrow", { width: 10, length: 30, location: 1 }],
                        ],
                    });

                    if (configs.editable) {
                        div.addEventListener("dblclick", () => {
                            eleSetter.makeVisible();
                            eleSetter.innerHTML = `
                                <div><span style="float: right; display: inline-block;"><button oId="deleteNode">删除节点</button><button oId="ok">确认</button></span></div>
                            `;

                            eleSetter.querySelector('button[oId="deleteNode"]').onclick = (evt) => {
                                that.instance.remove(div);
                                eleSetter.makeDispear();
                            }
                            eleSetter.querySelector('button[oId="ok"]').onclick = (evt) => {
                                eleSetter.makeDispear();
                            }
                        });
                    }
                    break;
                }
                case NodeType.END: {
                    option = option || {
                        offsetLeft: 30,
                        offsetTop: 30,
                        id: genId(),
                    };
                    div = document.createElement('div');
                    div.style.cssText = `
                    background:#494949; 
                    height: 100px; 
                    width: 100px; 
                    position: absolute;
                    box-shadow:0 2px 12px 0 #00d6fd;
                    border-radius:100px;
                    left: ${option.offsetLeft}px;
                    top: ${option.offsetTop}px;
                    `
                    drawPane.appendChild(div);
                    div.__flow_type = NodeType.END;
                    div.__flow_data = {
                        id: option.id,
                        name: 'end',
                    };
                    that.instance.addEndpoint(div, {
                        isTarget: true,
                        anchor: 'Left',
                        endpointStyle: { fill: "#fd4700" },
                        maxConnections: Number.MAX_SAFE_INTEGER,
                    });
                    if (configs.editable) {
                        div.addEventListener("dblclick", () => {
                            eleSetter.makeVisible();
                            eleSetter.innerHTML = `
                                <div><span style="float: right; display: inline-block;"><button oId="deleteNode">删除节点</button><button oId="ok">确认</button></span></div>
                            `;

                            eleSetter.querySelector('button[oId="deleteNode"]').onclick = (evt) => {
                                that.instance.remove(div);
                                eleSetter.makeDispear();
                            }
                            eleSetter.querySelector('button[oId="ok"]').onclick = (evt) => {
                                eleSetter.makeDispear();
                            }
                        });
                    }
                    break;
                }
            }

            that.instance.draggable(div);

            return div;
        }

        jsPlumb.ready(() => {
            that.instance = jsPlumb.getInstance({
                Connector: ["Flowchart", { cornerRadius: 0.5, stup: 10 }],
                Endpoint: ["Dot", { radius: 5 }],
                EndpointStyle: { fill: "green" },
            })
            that.instance.setContainer(drawPane);
            jsplumbisready = true;
        });

        this.getDefinition = () => {
            let definitions = new Map();
            let connectionMap = new Map();
            that.instance.select().each((connection) => {
                if (!connectionMap.get(connection.source.__flow_data.id)) {
                    connectionMap.set(connection.source.__flow_data.id, [connection]);
                } else {
                    connectionMap.get(connection.source.__flow_data.id).push(connection);
                }
            });
            that.instance.select().each((connection) => {
                let source = connection.source;
                let target = connection.target;

                switch (target.__flow_type) {
                    case NodeType.END: {
                        let data = target.__flow_data;
                        if (!definitions.get(data.id)) {
                            definitions.set(data.id, {
                                id: data.id,
                                name: "end",
                                type: target.__flow_type,
                                offsetLeft: target.style.left.replace('px', ''),
                                offsetTop: target.style.top.replace('px', ''),
                            });
                        }
                        break;
                    }
                }

                switch (source.__flow_type) {
                    case NodeType.START: {
                        let data = source.__flow_data;
                        if (!definitions.get(data.id)) {
                            definitions.set(data.id, {
                                id: data.id,
                                name: "start",
                                type: source.__flow_type,
                                nextNodeType: target.__flow_type,
                                nextNode: target.__flow_data.id,
                                offsetLeft: source.style.left.replace('px', ''),
                                offsetTop: source.style.top.replace('px', ''),
                            });
                        }
                        break;
                    }
                    case NodeType.AUDIT: {
                        let data = source.__flow_data;
                        let targetData = target.__flow_data;
                        let definition = null;
                        if (!definitions.get(data.id)) {
                            definition = {
                                id: data.id,
                                name: data.name,
                                type: source.__flow_type,
                                nextNodeType: target.__flow_type,
                                offsetLeft: source.style.left.replace('px', ''),
                                offsetTop: source.style.top.replace('px', ''),
                                auditor: data.auditor,
                            };
                            definitions.set(data.id, definition);
                        }
                        switch (target.__flow_type) {
                            case NodeType.EXCLUDE_GATEWAY: {
                                let allow, reject = null;
                                connectionMap.get(targetData.id).forEach((conn) => {
                                    if (conn.endpoints[0].anchor.type === 'Bottom') {
                                        if (targetData.downdot === 'reject') {
                                            reject = conn;
                                        } else {
                                            allow = conn;
                                        }
                                    } else {
                                        if (targetData.downdot === 'reject') {
                                            allow = conn;
                                        } else {
                                            reject = conn;
                                        }
                                    }
                                });
                                definition.allow = allow.target.__flow_data.id;
                                definition.reject = reject.target.__flow_data.id;
                                definition.excludeGateway = {
                                    offsetLeft: target.style.left.replace('px', ''),
                                    offsetTop: target.style.top.replace('px', ''),
                                    id: targetData.id,
                                    name: targetData.name,
                                    updot: targetData.updot,
                                    downdot: targetData.downdot,
                                }
                                break;
                            }
                            case NodeType.END:
                            case NodeType.AUDIT: {
                                definition.nextNode = targetData.id;
                                break;
                            }
                        }
                        break;
                    }
                }
            });

            let ans = [];
            definitions.forEach(v => ans.push(v));
            return ans;
        }

        this.drawWorkflow = (definitions) => {
            waitReady(() => {
                let idMap = new Map();
                let connections = [];
                definitions.forEach(definition => {
                    switch (definition.type) {
                        case NodeType.START: {
                            idMap.set(definition.id, genNode(NodeType.START, {
                                id: definition.id,
                                offsetLeft: definition.offsetLeft,
                                offsetTop: definition.offsetTop,
                            }));
                            connections.push({
                                source: definition.id,
                                target: definition.nextNode,
                                newConnection: false,
                                anchors: ["Right", "Left"],
                                endpointStyles: [
                                    null,
                                    { fill: "#fd4700" },
                                ],
                                overlays: [
                                    ["Arrow", { width: 10, length: 30, location: 1 }],
                                ]
                            })
                            break;
                        }
                        case NodeType.AUDIT: {
                            idMap.set(definition.id, genNode(NodeType.AUDIT, {
                                offsetLeft: definition.offsetLeft,
                                offsetTop: definition.offsetTop,
                                name: definition.name,
                                id: definition.id,
                                auditor: definition.auditor,
                            }));
                            switch (definition.nextNodeType) {
                                case NodeType.EXCLUDE_GATEWAY: {
                                    idMap.set(definition.excludeGateway.id, genNode(NodeType.EXCLUDE_GATEWAY, {
                                        offsetLeft: definition.excludeGateway.offsetLeft,
                                        offsetTop: definition.excludeGateway.offsetTop,
                                        id: definition.excludeGateway.id,
                                        name: definition.excludeGateway.name,
                                        updot: definition.excludeGateway.updot,
                                        downdot: definition.excludeGateway.downdot,
                                    }));
                                    connections.push({
                                        source: definition.id,
                                        target: definition.excludeGateway.id,
                                        newConnection: false,
                                        anchors: ["Right", "Left"],
                                        endpointStyles: [
                                            null,
                                            { fill: "#fd4700" },
                                        ],
                                        overlays: [
                                            ["Arrow", { width: 10, length: 30, location: 1 }],
                                        ]
                                    });

                                    connections.push({
                                        source: definition.excludeGateway.id,
                                        target: definition.allow,
                                        newConnection: false,
                                        anchors: [definition.excludeGateway.updot === 'allow' ? 'Top' : 'Bottom', "Left"],
                                        endpointStyles: [
                                            null,
                                            { fill: "#fd4700" },
                                        ],
                                        overlays: [
                                            ["Arrow", { width: 10, length: 30, location: 1 }],
                                            ["Label", { label: "Y", location: 0.1, id: "label" }],
                                        ]
                                    });

                                    connections.push({
                                        source: definition.excludeGateway.id,
                                        target: definition.reject,
                                        newConnection: false,
                                        anchors: [definition.excludeGateway.downdot === 'reject' ? 'Bottom' : 'Top', "Left"],
                                        endpointStyles: [
                                            null,
                                            { fill: "#fd4700" },
                                        ],
                                        overlays: [
                                            ["Arrow", { width: 10, length: 30, location: 1 }],
                                            ["Label", { label: "N", location: 0.1, id: "label" }],
                                        ]
                                    });

                                    break;
                                }
                                case NodeType.AUDIT: {
                                    connections.push({
                                        source: definition.id,
                                        target: definition.nextNode,
                                        newConnection: false,
                                        anchors: ["Right", "Left"],
                                        endpointStyles: [
                                            null,
                                            { fill: "#fd4700" },
                                        ],
                                        overlays: [
                                            ["Arrow", { width: 10, length: 30, location: 1 }],
                                        ]
                                    })
                                    break;
                                }
                                case NodeType.END: {
                                    connections.push({
                                        source: definition.id,
                                        target: definition.nextNode,
                                        newConnection: false,
                                        anchors: ["Right", "Left"],
                                        endpointStyles: [
                                            null,
                                            { fill: "#fd4700" },
                                        ],
                                        overlays: [
                                            ["Arrow", { width: 10, length: 30, location: 1 }],
                                        ]
                                    })
                                    break;
                                }
                            }
                            break;
                        }
                        case NodeType.END: {
                            idMap.set(definition.id, genNode(NodeType.END, {
                                id: definition.id,
                                offsetLeft: definition.offsetLeft,
                                offsetTop: definition.offsetTop,
                            }));
                            break;
                        }
                    }
                });
                that.instance.batch(() => {
                    connections.forEach((it) => {
                        it.source = idMap.get(it.source);
                        it.target = idMap.get(it.target);
                        that.instance.connect(it);
                    });
                });
            });
        }

        this.setCurrentNode = (id) => {
            waitReady(() => {
                let redShadow = "box-shadow: rgb(255, 3, 3) 0px 2px 12px 0px;";
                that.instance.select().each(connection => {
                    [connection.source, connection.target].forEach(item => {
                        let data = item.__flow_data;
                        let type = item.__flow_type;

                        switch (type) {
                            case NodeType.EXCLUDE_GATEWAY: {
                                item = item.children[0];
                                break;
                            }
                        }

                        if (item.style.cssText.indexOf(redShadow)) {
                            item.style.cssText = item.style.cssText.replace(redShadow, 'box-shadow:0 2px 12px 0 #00d6fd;');
                        }
                        if (data.id === id) {
                            item.style.cssText += redShadow;
                        }
                    });
                });
            });
        }

        this.regisGetUsers = (call) => {
            getUsers = call;
        }

    }

    return Flow;
})());

export {
    Flow,
}