/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.struct

import net.rwhps.server.func.ConsSeq
import net.rwhps.server.func.Control.ControlFind
import net.rwhps.server.func.FindSeq
import net.rwhps.server.util.ExtractUtils

/**
 * @date  2023/5/26 13:55
 * @author  RW-HPS/Dr
 *
 * @param T Type
 * @property list ObjectList<T>
 * @property threadSafety ThreadSafety
 * @property size Int
 */
@Suppress("UNUSED", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
abstract class BaseSeq<T>(private val list: java.util.List<T>, private val threadSafety: Boolean): MutableList<T>, List<T> {

    override val size: Int get() = list.size
    override fun isEmpty(): Boolean = list.isEmpty()

    override fun add(element: T): Boolean = list.add(element)
    override fun add(index: Int, element: T) = list.add(index, element)
    override fun addAll(elements: Collection<T>): Boolean = list.addAll(elements)
    override fun addAll(index: Int, elements: Collection<T>): Boolean = list.addAll(index, elements)

    /**
     * 移除最后一个元素
     * @return T
     */
    fun pop(): T = removeAt(size - 1)
    /**
     * 获取最后一个元素
     * @return T
     */
    fun peek(): T = get(size - 1)
    /**
     * 获取第一个元素
     * @return T
     */
    fun first(): T = get(0)
    /**
     * 移除第一个元素
     * @return T
     */
    fun removeFirst(): T = removeAt(0)
    override fun iterator(): MutableIterator<T> = list.iterator()
    override fun listIterator(): MutableListIterator<T> = list.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = list.listIterator(index)
    fun any(): Boolean = list.isEmpty()
    override fun get(index: Int): T = list[index]
    override fun indexOf(element: @UnsafeVariance T): Int = list.indexOf(element)
    override fun lastIndexOf(element: @UnsafeVariance T): Int = list.lastIndexOf(element)
    override fun remove(element: T): Boolean = list.remove(element)
    override fun removeAt(index: Int): T = list.remove(index)
    override fun removeAll(elements: Collection<T>): Boolean = list.removeAll(elements.toSet())
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = list.subList(fromIndex, toIndex)
    override fun set(index: Int, element: T): T = list.set(index, element)
    override fun retainAll(elements: Collection<T>): Boolean = list.retainAll(elements.toSet())
    override fun contains(element: T): Boolean = list.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)

    fun find(findA: FindSeq<T, Boolean>): T? {
        var result: T? = null
        ExtractUtils.synchronizedX(threadSafety, list) {
            list.forEach { if (findA(it)) { result = it } }
        }
        return result
    }

    fun find(findA: FindSeq<T, Boolean>, findB: FindSeq<T, Boolean>): T? {
        var result: T? = null
        ExtractUtils.synchronizedX(threadSafety, list) {
            list.forEach { if (findA(it) && findB(it)) { result = it } }
        }
        return result
    }

    fun eachAll(block: ConsSeq<T>) {
        ExtractUtils.synchronizedX(threadSafety, list) {
            list.forEach { block(it) }
        }
    }

    fun eachControlAll(findA: FindSeq<T, ControlFind>) {
        ExtractUtils.synchronizedX(threadSafety, list) {
            list.forEach {
                if (findA(it) == ControlFind.BREAK) {
                    return@synchronizedX
                }
            }
        }
    }

    fun eachAllFind(find: FindSeq<T, Boolean>, block: ConsSeq<T>) = eachAll {
        if (find(it)) {
            block(it)
        }
    }

    fun eachFind(find: FindSeq<T, Boolean>, block: ConsSeq<T>) = find(find)?.let { block(it) }
    fun eachAllFinds(findA: FindSeq<T, Boolean>, findB: FindSeq<T, Boolean>, block: ConsSeq<T>) = find(findA, findB)?.let { block(it) }

    override fun clear() = list.clear()

    @Suppress("UNCHECKED_CAST")
    fun <E> toArray(classJava: Class<E>): Array<E> = list.toArray(java.lang.reflect.Array.newInstance(classJava, size) as Array<out E>)

    override fun toString(): String = list.toString()
}