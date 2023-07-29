<template>
    这是文件浏览器，当前路径：

    <span>
        <span class="path-segment" v-for="seg in directories" @click="onClickPath(seg)">{{ seg.name }}</span>
    </span>

    <File v-for="file in list" @on-click="onClickFile" :file="file"></File>
</template>

<script setup lang="ts">
import { Ref, onMounted, ref } from 'vue';
import File from './File.vue'
import NetworkException from '../exceptions/NetworkException'
import FileInfo from '../data/FileInfo'

interface PathSeg {
    name: string,
    path: string,
    level: number,
}

const list: Ref<Array<FileInfo>> = ref([])
const directories: Ref<Array<PathSeg>> = ref([{ name: 'Home', path: '', level: 0 }])

onMounted(() => {
    update()
})

function update() {
    (async () => {
        let response = await fetch('http://127.0.0.1:6800/explorer/list/' + cwd(), {
            cache: 'no-cache',
            mode: 'cors',
        })

        if (!response.ok)
            throw new NetworkException(response.statusText)
        
        list.value = []

        for (const file of JSON.parse(await response.text())) {
            list.value.push({
                name: file['name'],
                length: file['length'],
                modified: file['modified'],
            })
        }

        list.value.sort((a, b) => {
            let adir = a.length == -1
            let bdir = b.length == -1
            
            return adir != bdir ? (adir ? -1 : 1) : a.name.localeCompare(b.name)
        })

    })()
}

function onClickFile(info: FileInfo) {
    // console.log(info);

    if (info.length == -1)
    {
        let path = directories.value.slice(1).map(s => s.name).concat(info.name).join('/')
        directories.value.push({ name: info.name, path: path, level: directories.value.length })
        update()
    }
}

function onClickPath(seg: PathSeg) {
    let count = directories.value.length - 1 - seg.level

    for (let i = 0; i < count; i++)
        directories.value.pop()

    update()
}

function cwd(): string {
    return directories.value.slice(1).map(s => s.name).join('/')
}

defineExpose({
    onClickFile
})

</script>


<style scoped lang="stylus">
    .path-segment
        &:hover
            background: #e7f345

        &:not(:last-child)
            // margin-right: 1rem

            &:after
                content: "/"
</style>