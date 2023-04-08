package mcpatch.stream

import java.io.ByteArrayInputStream

class MemoryInputStream(buf: MemoryOutputStream) : ByteArrayInputStream(buf.buffer(), 0, buf.size())
{

}