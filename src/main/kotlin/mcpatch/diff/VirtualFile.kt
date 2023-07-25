package mcpatch.diff

import org.json.JSONArray
import org.json.JSONObject
import mcpatch.utility.File2

/**
 * 虚拟文件对象
 */
class VirtualFile : ComparableFile
{
    val parent: VirtualFile?
    override var name: String
    override var length: Long
    override var hash: String
    override var modified: Long
    override var files: MutableList<VirtualFile>
    override val isFile: Boolean
    override val relativePath: String

    /**
     * 创建一个虚拟"文件"对象
     */
    constructor(name: String, length: Long, hash: String, modified: Long, parent: VirtualFile?)
    {
        this.parent = parent
        this.name = name
        this.length = length
        this.hash = hash
        this.modified = modified
        files = mutableListOf()
        isFile = length != -1L
        relativePath = (if (parent != null) parent.relativePath + "/" else "") + name
    }

    /**
     * 创建一个虚拟"目录"对象
     */
    constructor(name: String, files: MutableList<VirtualFile>, parent: VirtualFile?)
    {
        this.parent = parent
        this.name = name
        this.length = -1
        this.hash = "not a file"
        this.modified = -1
        this.files = files
        isFile = length != -1L
        relativePath = (if (parent != null) parent.relativePath + "/" else "") + name
    }

//    /**
//     * 删除一个文件
//     */
//    fun removeFile(relativePath: String)
//    {
//        if (isFile)
//            throw InvalidObjectException("the file named '$name' is not a directory, is a file.")
//
//        val split = relativePath.replace("\\", "/").split("/")
//        var currentDir = this
//
//        for ((index, name) in split.withIndex())
//        {
//            val reachEnd = index == split.size - 1
//            val current = currentDir.files.first { it.name == name }
//            if (!reachEnd) currentDir = current else currentDir.files.removeIf { it.name == name }
//        }
//    }

//    /**
//     * 深拷贝当前对象
//     */
//    fun clone(): VirtualFile
//    {
//        fun c(vf: VirtualFile, _parent: VirtualFile?): VirtualFile
//        {
//            return if (isFile)
//                VirtualFile(vf.name, vf.length, vf.hash, vf.modified, _parent)
//            else
//                VirtualFile(vf.name, mutableListOf(), _parent)
//                    .also { v -> v.files.addAll(vf.files.map { c(it, v) }) }
//        }
//
//        return c(this, parent)
//    }

//    /**
//     * 将当前VirualFile序列号为JsonObject
//     */
//    fun toJsonObject(): JSONObject
//    {
//        val json = JSONObject()
//        json.put("name", name)
//
//        if (isFile)
//        {
//            json.put("length", length)
//            json.put("hash", hash)
//            json.put("modified", modified)
//        } else {
//            val files = JSONArray()
//            for (child in this.files)
//                files.put(child.toJsonObject())
//            json.put("files", files)
//        }
//
//        return json
//    }

//    /**
//     * 从真实文件对象更新自己
//     * @param diff Diff对象
//     * @param source 参照目录
//     */
//    fun applyDiff(diff: DirectoryDiff, source: File2)
//    {
//        for (f in diff.redundantFiles)
//        {
//            removeFile(f)
//        }
//
//        for (f in diff.redundantFolders)
//        {
//            removeFile(f)
//        }
//
//        for (f in diff.missingFolders)
//        {
//            val parent = PathUtils.getDirPathPart(f)
//            val filename = PathUtils.getFileNamePart(f)
//
//            val dir = if (parent != null) this.get(parent)!! as VirtualFile else this
//            dir.files += VirtualFile(filename, mutableListOf(), dir)
//        }
//
//        for (f in diff.missingFiles)
//        {
//            val parent = PathUtils.getDirPathPart(f)
//            val filename = PathUtils.getFileNamePart(f)
//
//            val dir = if (parent != null) this.get(parent)!! as VirtualFile else this
//            val file = source + f
//            val length = file.length
//            val modified = file.modified
//            val hash = HashUtils.crc32(file.file)
//
//            dir.files += VirtualFile(filename, length, hash, modified, dir)
//        }
//    }

    companion object {
        /**
         * 从Json文件读取VirtualFiles
         * @param jsonFile 从哪个文件读取
         * @return 读取并解析的VirtualFiles。如果文件不存在则返回Null
         */
        @JvmStatic
        fun FromJsonFile(jsonFile: File2): List<VirtualFile>?
        {
            if (!jsonFile.exists)
                return null

            return FromJsonArray(JSONArray(jsonFile.content))
        }

        /**
         * 从JsonArray对象创建VirtualFiles
         * @param jsonArray JsonArray对象
         * @return 创建好的VirtualFiles
         */
        @JvmStatic
        fun FromJsonArray(jsonArray: JSONArray): List<VirtualFile> {
            fun parseAsLong(number: Any): Long = (number as? Int)?.toLong() ?: number as Long

            fun gen(f: JSONObject, parent: VirtualFile?): VirtualFile {
                val _name = f["name"] as String

                return if (f.has("files")) {
                    val files = f["files"] as JSONArray
                    VirtualFile(_name, mutableListOf(), parent)
                        .also { vf -> vf.files.addAll(files.map { gen(it as JSONObject, vf) }) }
                } else {
                    val length = parseAsLong(f["length"])
                    val hash = f["hash"] as String
                    val modified = parseAsLong(f["modified"])
                    VirtualFile(_name, length, hash, modified, parent)
                }
            }

            return jsonArray.map { gen(it as JSONObject, null) }
        }

        /**
         * 将VirtualFiles外面再套一层VirtualFile方便做数据处理
         * @param name 包裹层的VirtualFile名字，一般名字不重要可以留空
         * @param files 要被包裹的VirtualFiles
         */
        @JvmStatic
        fun WrapWithFolder(name: String, files: List<VirtualFile>): VirtualFile
        {
            return VirtualFile(name, mutableListOf(), null)
                .also { it.files.addAll(files) }
        }
    }
}

//@JvmStatic
//fun FromRealFile(file: File2): VirtualCF
//{
//    fun gen(file: File2, parent: VirtualCF?): VirtualCF
//    {
//        return if (file.isDirectory) {
//            VirtualCF(file.name, mutableListOf(), parent)
//                .also { vf -> file.files.map { gen(it, vf) } }
//        } else {
//            VirtualCF(file.name, file.length, HashUtils.crc32(file.file), file.modified, parent)
//        }
//    }
//
//    return gen(file, null)
//}