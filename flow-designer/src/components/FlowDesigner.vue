<template>
  <div
    ref="flowDesigner"
    style="position: relative; background: white; width: 100vw; height: 100vh"
  ></div>
</template>

<script>
import { Flow } from "@/others/Flow.js";

export default {
  name: "FlowDesigner",
  data() {
    return {
      flowPlumb: null,
    };
  },
  mounted() {
    this.flowPlumb = new Flow(this.$refs.flowDesigner, { editable: true });
  },
  methods: {},
};
</script>