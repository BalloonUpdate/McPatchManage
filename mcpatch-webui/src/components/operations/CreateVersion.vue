<template>
    这是创建版本
    1. 计算文件修改
    2. 提示输入版本号
    3. 粘贴更新记录
    4. 成功

    <div v-if="stage == 0">正在计算文件修改</div>

    <div v-if="stage == 1">
        输入新的版本号
        <input type="text" />
        <button type="button">下一步</button>
    </div>

    <div v-if="stage == 2">
        粘贴更新记录
        <textarea></textarea>
        <button type="button">下一步</button>
    </div>

    <div v-if="stage == 3">
        创建成功
    </div>

</template>

<script setup lang="ts">
import { ref } from 'vue';


const stage = ref(0)

</script>