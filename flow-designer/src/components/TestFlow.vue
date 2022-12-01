<template>
  <div id="container">
    <div class="col1">
      <div id="processDefineDiv_node_2" name="cell">派单</div>
    </div>
    <div class="col2">
      <div
        v-for="item in list2"
        :key="item.nodeId"
        :id="item.nodeId"
        name="cell"
      >
        {{ item.name }}
      </div>
    </div>
    <div class="col3">
      <div id="processDefineDiv_node_6" name="cell">完成</div>
    </div>
  </div>
</template>

<script>
import { jsPlumb as jb } from "jsplumb";

export default {
  name: "TestFlow",
  data() {
    return {
      jsPlumb: null,
      list2: [
        { name: "省公司虚拟专家组", nodeId: "processDefineDiv_node_3" },
        { name: "分公司咨询组", nodeId: "processDefineDiv_node_5" },
        { name: "县咨询组", nodeId: "processDefineDiv_node_4" },
      ], // 节点参数
      connlist: [
        {
          sourceNodeId: "processDefineDiv_node_2",
          targetNodeId: "processDefineDiv_node_3",
        },
        {
          sourceNodeId: "processDefineDiv_node_2",
          targetNodeId: "processDefineDiv_node_5",
        },
        // { sourceNodeId: 'processDefineDiv_node_2', targetNodeId: 'processDefineDiv_node_4' },
        {
          sourceNodeId: "processDefineDiv_node_3",
          targetNodeId: "processDefineDiv_node_5",
        },
        {
          sourceNodeId: "processDefineDiv_node_5",
          targetNodeId: "processDefineDiv_node_3",
        },
        {
          sourceNodeId: "processDefineDiv_node_3",
          targetNodeId: "processDefineDiv_node_6",
        },
        {
          sourceNodeId: "processDefineDiv_node_5",
          targetNodeId: "processDefineDiv_node_4",
        },
        {
          sourceNodeId: "processDefineDiv_node_4",
          targetNodeId: "processDefineDiv_node_5",
        },
        {
          sourceNodeId: "processDefineDiv_node_5",
          targetNodeId: "processDefineDiv_node_6",
        },
        {
          sourceNodeId: "processDefineDiv_node_4",
          targetNodeId: "processDefineDiv_node_6",
        },
      ], // 指定需要连接的两节点
    };
  },
  mounted() {
    jsPlumb.ready(() => {
      this.jsPlumb = jb.getInstance({
        Container: "container", // 选择器id
        EndpointStyle: { radius: 0.11, fill: "#fff" }, // 端点样式
        // PaintStyle: { stroke: '#00ff00', strokeWidth: 2 }, // 绘画样式，默认8px线宽  #456
        // HoverPaintStyle: { stroke: '#1E90FF' }, // 默认悬停样式  默认为null
        // EndpointHoverStyle: { fill: '#F00', radius: 6 }, // 端点悬停样式
        // ConnectionOverlays: [ // 此处可以设置所有箭头的样式，因为我们要改变连接线的样式，故单独配置
        //   ['Arrow', { // 设置参数可以参考中文文档
        //     location: 1,
        //     length: 10,
        //     paintStyle: {
        //       stroke: '#496def',
        //       fill: '#496def'
        //     }
        //   }]
        // ],
        Connector: ["Straight", { gap: 1 }], // 要使用的默认连接器的类型：直线，折线，曲线等
        DrapOptions: { cursor: "crosshair", zIndex: 2000 },
      });

      //后台参考数据
      this.oncedata = [
        {
          proc_inst_task_code: "processDefineDiv_node_2",
          proc_inst_task_name: "派单",
        },
        {
          proc_inst_task_code: "processDefineDiv_node_3",
          proc_inst_task_name: "省公司虚拟专家组",
        },
        {
          proc_inst_task_code: "processDefineDiv_node_5",
          proc_inst_task_name: "分公司咨询组",
        },
      ];
      this.oncedata.forEach((item, index) => {
        const ele = document.getElementById(item.proc_inst_task_code);
        ele.style.backgroundColor = "#10af10"; // 在流转数据中的节点都改为绿色背景
        if (index === this.oncedata.length - 1) {
          ele.style.backgroundColor = "#f11818"; // 最后一个节点是最终状态改为红色背景
        }
      });

      const ins = this.jsPlumb;
      ins.getAllConnections();
      ins.batch(() => {
        this.initAll();
        this.connectionAll();
      });
      this.switchContainer(true, true, false);
      this.oncedata.forEach((item, index) => {
        if (index < this.oncedata.length - 1) {
          const connection = this.jsPlumb.connect({
            // 对流程数据中对应的节点连接线重新绘制
            source: item.proc_inst_task_code,
            target: this.oncedata[++index].proc_inst_task_code,
            overlays: [
              [
                "Arrow",
                {
                  width: 12,
                  length: 10,
                  location: 1,
                  paintStyle: {
                    stroke: "#10af10",
                    fill: "#10af10",
                  },
                },
              ],
            ],
          });
          connection.setPaintStyle({ stroke: "#10af10", strokeWidth: 2 });
        }
      });
    });
  },
  methods: {
    initAll() {
      // 初始化所有节点
      this.init("processDefineDiv_node_2");
      this.init("processDefineDiv_node_6");
      const rl2 = this.list2;
      for (let i = 0; i < rl2.length; i++) {
        this.init(rl2[i].nodeId);
      }
    },
    // 初始化规则使其可以连线、拖拽
    init(id) {
      const ins = this.jsPlumb;
      const elem = document.getElementById(id);
      ins.makeSource(elem, {
        anchor: ["Perimeter", { anchorCount: 200, shape: "Rectangle" }],
        allowLoopback: false,
        maxConnections: 1,
      });
      ins.makeTarget(elem, {
        anchor: ["Perimeter", { anchorCount: 200, shape: "Rectangle" }],
        allowLoopback: false,
        maxConnections: 1,
      });
    },
    connectionAll() {
      const ins = this.jsPlumb;
      ins.ready(() => {
        // 入口
        for (let i = 0; i < this.connlist.length; i++) {
          const conn = this.connlist[i];
          const connection = ins.connect({
            source: conn.sourceNodeId,
            target: conn.targetNodeId,
            overlays: [
              [
                "Arrow",
                {
                  width: 12,
                  length: 10,
                  location: 1,
                  paintStyle: {
                    stroke: "#496def",
                    fill: "#496def",
                  },
                },
              ],
            ],
          });
          connection.setPaintStyle({ stroke: "#496def", strokeWidth: 2 });
        }
      });
    },
    switchContainer(target, source, draggable) {
      const elem = document.getElementsByName("cell");
      const ins = this.jsPlumb;
      ins.setSourceEnabled(elem, source);
      ins.setTargetEnabled(elem, target);
      ins.setDraggable(elem, draggable); // 是否支持拖拽
    },
  },
};
</script>

<style>
#container {
  position: relative;
  width: 100%;
  padding: 0 50px;
}
.col2,
.col1 {
  float: left;
  text-align: center;
}
.col3 {
  float: right;
  text-align: center;
}
.col1 {
  width: 180px;
}
.col2 {
  width: 200px;
  margin: 0 200px;
}
.col3 {
  width: 180px;
}
#container > div > div {
  line-height: 40px;
  background: #496def;
  margin: 50px 0;
}
</style>