export default abstract class BaseException extends Error {
    constructor(message: string) 
    {
        super()
        this.name = this.constructor.name
        this.message = message

        // 获取堆栈
        // let obj = Object.create(null)
        // Error.captureStackTrace(obj)
        // let stack = obj.stack as string

        // let stack = this.stack ?? 'no stack traceback'
        // let firstLineend = stack.indexOf('\n')
        // stack = stack.substring(firstLineend + 1).trim()
        
        // window.bubble(message.toString(), 15000)
    }
}