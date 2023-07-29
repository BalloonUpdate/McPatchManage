<template>
    <div ref="terminal"></div>
</template>


<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Terminal } from "xterm";
import 'xterm/css/xterm.css'

const terminal = ref(null)
const xterm = ref(new Terminal())

onMounted(() => {
    xterm.value.open(terminal.value!)
    xterm.value.writeln('哼，到时候用这个web命令行来折磨大家')
    xterm.value.writeln('')

    xterm.value.onKey((key, data) => {
        console.log(key);
    })
})
</script>