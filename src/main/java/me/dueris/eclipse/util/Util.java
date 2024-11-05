package me.dueris.eclipse.util;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collector;

public class Util {

	public static final String ECLIPSE = "eclipse";
	public static final String VANILLA = "paper";
	static final Set<Collector.Characteristics> CH_ID
		= Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

	public static void consumePaperClipList(Consumer<String> lineConsumer, JarEntry entry, JarFile jarFile) throws Throwable {
		try (final InputStream inputStream = jarFile.getInputStream(entry); final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] values = line.split("\t");

				if (values.length >= 3) {
					lineConsumer.accept(values[2]);
				}
			}
		}
	}

	public static <T> @NotNull Collector<T, ?, LinkedHashSet<T>> toLinkedSet() {
		return new CollectorImpl<>(LinkedHashSet::new, Set::add,
			(left, right) -> {
				if (left.size() < right.size()) {
					right.addAll(left);
					return right;
				} else {
					left.addAll(right);
					return left;
				}
			}, CH_ID);
	}

	@SuppressWarnings("unchecked")
	private static <I, R> @NotNull Function<I, R> castingIdentity() {
		return i -> (R) i;
	}

	public static String insertBranding(final String brand) {
		if (brand == null || brand.isEmpty()) {
			Logger.warn("Null or empty branding found!", new IllegalStateException());
			return ECLIPSE;
		}

		return VANILLA.equals(brand) ? ECLIPSE : brand + ',' + ECLIPSE;
	}

	record CollectorImpl<T, A, R>(Supplier<A> supplier,
								  BiConsumer<A, T> accumulator,
								  BinaryOperator<A> combiner,
								  Function<A, R> finisher,
								  Set<Characteristics> characteristics
	) implements Collector<T, A, R> {

		CollectorImpl(Supplier<A> supplier,
					  BiConsumer<A, T> accumulator,
					  BinaryOperator<A> combiner,
					  Set<Characteristics> characteristics) {
			this(supplier, accumulator, combiner, castingIdentity(), characteristics);
		}
	}
}
