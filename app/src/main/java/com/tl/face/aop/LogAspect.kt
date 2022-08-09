package com.tl.face.aop

import android.os.Looper
import android.os.Trace
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.CodeSignature
import org.aspectj.lang.reflect.MethodSignature
import timber.log.Timber
import java.util.concurrent.TimeUnit


@Suppress("unused")
@Aspect
class LogAspect {

    @Pointcut("execution(@com.tl.face.aop.Log *.new(..))")
    fun constructor() {}


    @Pointcut("execution(@com.tl.face.aop.Log * *(..))")
    fun method() {}


    @Around("(method() || constructor()) && @annotation(log)")
    @Throws(Throwable::class)
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint, log: Log): Any? {
        enterMethod(joinPoint, log)
        val startNanos: Long = System.nanoTime()
        val result: Any? = joinPoint.proceed()
        val stopNanos: Long = System.nanoTime()
        exitMethod(joinPoint, log, result, TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos))
        return result
    }


    private fun enterMethod(joinPoint: ProceedingJoinPoint, log: Log) {
        val codeSignature: CodeSignature = joinPoint.signature as CodeSignature


        val className: String = codeSignature.declaringType.name

        val methodName: String = codeSignature.name

        val parameterNames: Array<String?> = codeSignature.parameterNames

        val parameterValues: Array<Any?> = joinPoint.args


        val builder: StringBuilder =
            getMethodLogInfo(className, methodName, parameterNames, parameterValues)
        log(log.value, builder.toString())
        val section: String = builder.substring(2)
        Trace.beginSection(section)
    }

    /**
     * 获取方法的日志信息
     *
     * @param className
     * @param methodName
     * @param parameterNames
     * @param parameterValues
     */
    private fun getMethodLogInfo(className: String, methodName: String, parameterNames: Array<String?>, parameterValues: Array<Any?>): StringBuilder {
        val builder: StringBuilder = StringBuilder("\u21E2 ")
        builder.append(className)
            .append(".")
            .append(methodName)
            .append('(')
        for (i in parameterValues.indices) {
            if (i > 0) {
                builder.append(", ")
            }
            builder.append(parameterNames[i]).append('=')
            builder.append(parameterValues[i].toString())
        }
        builder.append(')')
        if (Looper.myLooper() != Looper.getMainLooper()) {
            builder.append(" [Thread:\"").append(Thread.currentThread().name).append("\"]")
        }
        return builder
    }

    /**
     * @param result
     * @param lengthMillis
     */
    private fun exitMethod(joinPoint: ProceedingJoinPoint, log: Log, result: Any?, lengthMillis: Long) {
        Trace.endSection()
        val signature: Signature = joinPoint.signature
        val className: String? = signature.declaringType.name
        val methodName: String? = signature.name
        val builder: StringBuilder = StringBuilder("\u21E0 ")
            .append(className)
            .append(".")
            .append(methodName)
            .append(" [")
            .append(lengthMillis)
            .append("ms]")

        if (signature is MethodSignature && signature.returnType != Void.TYPE) {
            builder.append(" = ")
            builder.append(result.toString())
        }
        log(log.value, builder.toString())
    }

    private fun log(tag: String?, msg: String?) {
        Timber.tag(tag)
        Timber.d(msg)
    }
}