<template>
    <button @click="switchTab(0)">文件浏览器</button>
    <button @click="switchTab(1)">创建新版本</button>
    <button @click="switchTab(2)">合并版本</button>
    <button @click="switchTab(3)">还原工作空间</button>
    <button @click="switchTab(4)">还原历史目录</button>
    <button @click="switchTab(5)">删除所有历史版本</button>

    <hr/>
    <FileExplorer v-if="tabIndex == 0"></FileExplorer>
    <CreateVersion v-if="tabIndex == 1"></CreateVersion>
    <CombineVersions v-if="tabIndex == 2"></CombineVersions>
    <RestoreWorkspace v-if="tabIndex == 3"></RestoreWorkspace>
    <RevertHistory v-if="tabIndex == 4"></RevertHistory>
    <ClearAllVersions v-if="tabIndex == 5"></ClearAllVersions>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import FileExplorer from './components/FileExplorer.vue'
import CreateVersion from './components/operations/CreateVersion.vue'
import CombineVersions from './components/operations/CombineVersions.vue'
import RestoreWorkspace from './components/operations/RestoreWorkspace.vue'
import RevertHistory from './components/operations/RevertHistory.vue'
import ClearAllVersions from './components/operations/ClearAllVersions.vue'

const tabIndex = ref(0)

function switchTab(index: number) {
    tabIndex.value = index
}

</script>

<style scoped lang="stylus">
</style>
