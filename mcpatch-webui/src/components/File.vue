<template>
    <div class="file-info" @click="$emit('on-click', file)">
        {{ file.name }}
    </div>
</template>

<script setup lang="ts">
import FileInfo from '../data/FileInfo';

// const file: Ref<FileInfo> = ref(null!)

defineProps<{
    file: FileInfo
}>()

defineEmits<{
    (event: 'on-click', info: FileInfo): void
}>()

</script>

<style scoped lang="stylus">
    .file-info:hover
        background: #5edf92

</style>