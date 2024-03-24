/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.alphamode.wisp.util;

import me.alphamode.wisp.FileSystemUtil;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class ZipUtils { // Yes I stole from loom again

	public static int transform(Path zip, Collection<Pair<String, UnsafeUnaryOperator<byte[]>>> transforms) {
		try {
			return transform(zip, transforms.stream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int transform(Path zip, Stream<Pair<String, UnsafeUnaryOperator<byte[]>>> transforms) throws IOException {
		return transform(zip, collectTransformersStream(transforms));
	}

	public static <T> int transformMapped(Path zip, Map<String, UnsafeUnaryOperator<T>> transforms, Function<byte[], T> deserializer, Function<T, byte[]> serializer) throws IOException {
		Map<String, UnsafeUnaryOperator<byte[]>> newTransforms = new HashMap<>();

		for (Map.Entry<String, UnsafeUnaryOperator<T>> entry : transforms.entrySet()) {
			if (entry.getValue() != null) {
				newTransforms.put(entry.getKey(), bytes -> {
					return serializer.apply(entry.getValue().apply(deserializer.apply(bytes)));
				});
			}
		}

		return transform(zip, newTransforms);
	}

	public static int transform(Path zip, Map<String, UnsafeUnaryOperator<byte[]>> transforms) throws IOException {
		int replacedCount = 0;

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(zip, false)) {
			for (Map.Entry<String, UnsafeUnaryOperator<byte[]>> entry : transforms.entrySet()) {
				Path fsPath = fs.get().getPath(entry.getKey());

				if (Files.exists(fsPath) && entry.getValue() != null) {
					Files.write(fsPath, entry.getValue().apply(Files.readAllBytes(fsPath)), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
					replacedCount++;
				}
			}
		}

		return replacedCount;
	}

	@FunctionalInterface
	public interface UnsafeUnaryOperator<T> {
		T apply(T arg) throws IOException;
	}

	public interface AsmClassOperator extends UnsafeUnaryOperator<byte[]> {
		ClassVisitor visit(ClassVisitor classVisitor);

		@Override
		default byte[] apply(byte[] arg) throws IOException {
			final ClassReader reader = new ClassReader(arg);
			final ClassWriter writer = new ClassWriter(0);

			reader.accept(visit(writer), 0);

			return writer.toByteArray();
		}
	}

	private static <T> Map<String, UnsafeUnaryOperator<T>> collectTransformersStream(Stream<Pair<String, UnsafeUnaryOperator<T>>> transforms) {
		Map<String, UnsafeUnaryOperator<T>> map = new HashMap<>();
		Iterator<Pair<String, UnsafeUnaryOperator<T>>> iterator = transforms.iterator();

		while (iterator.hasNext()) {
			Pair<String, UnsafeUnaryOperator<T>> next = iterator.next();
			map.put(next.first(), next.second());
		}

		return map;
	}
}
