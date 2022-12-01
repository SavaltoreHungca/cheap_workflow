<template>
  <div id="app">
    <FlowDesigner/>
  </div>
</template>

<script>
import TestFlow from "@/components/TestFlow";
import FlowDesigner from "@/components/FlowDesigner";

export default {
  name: "App",
  components: {
    TestFlow,
    FlowDesigner,
  },
};
</script>

<style>
body {
  margin: 0;
  padding: 0;
}
</style>
