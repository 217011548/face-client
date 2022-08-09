package com.tl.face.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.CodeSignature
import timber.log.Timber


@Suppress("unused")
@Aspect
class SingleClickAspect {


    private var lastTime: Long = 0


    private var lastTag: String? = null


    @Pointcut("execution(@com.tl.face.aop.SingleClick * *(..))")
    fun method() {}


    @Around("method() && @annotation(singleClick)")
    @Throws(Throwable::class)
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint, singleClick: SingleClick) {
        val codeSignature: CodeSignature = joinPoint.signature as CodeSignature

        val className: String = codeSignature.declaringType.name

        val methodName: String = codeSignature.name

        val builder: StringBuilder = StringBuilder("$className.$methodName")
        builder.append("(")
        val parameterValues: Array<Any?> = joinPoint.args
        for (i in parameterValues.indices) {
            val arg: Any? = parameterValues[i]
            if (i == 0) {
                builder.append(arg)
            } else {
                builder.append(", ")
                    .append(arg)
            }
        }
        builder.append(")")
        val tag: String = builder.toString()
        val currentTimeMillis: Long = System.currentTimeMillis()
        if (currentTimeMillis - lastTime < singleClick.value && (tag == lastTag)) {
            Timber.tag("SingleClick")
            Timber.i("%s click within：%s", singleClick.value, tag)
            return
        }
        lastTime = currentTimeMillis
        lastTag = tag

        joinPoint.proceed()
    }
}