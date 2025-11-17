package org.lokray.util;

import java.util.function.Function;

/**
 * A sealed interface representing a value that is either Ok (success) or Err (failure).
 * This is a functional alternative to throwing exceptions or returning null.
 *
 * @param <T> The type of the success value.
 * @param <E> The type of the error value.
 */
public sealed interface Result<T, E>
{
	/**
	 * Creates a new success (Ok) result.
	 *
	 * @param value The success value.
	 * @return An Ok result holding the value.
	 */
	static <T, E> Result<T, E> ok(T value)
	{
		return new Ok<>(value);
	}

	/**
	 * Creates a new failure (Err) result.
	 *
	 * @param error The error value.
	 * @return An Err result holding the error.
	 */
	static <T, E> Result<T, E> err(E error)
	{
		return new Err<>(error);
	}

	/**
	 * @return true if the result is Ok, false otherwise.
	 */
	boolean isOk();

	/**
	 * @return true if the result is Err, false otherwise.
	 */
	boolean isErr();

	/**
	 * Gets the success value.
	 *
	 * @return The success value.
	 * @throws RuntimeException if called on an Err result.
	 */
	T unwrap() throws RuntimeException;

	/**
	 * Gets the error value.
	 *
	 * @return The error value.
	 * @throws RuntimeException if called on an Ok result.
	 */
	E unwrapErr() throws RuntimeException;

	/**
	 * Gets the success value, or a default value if the result is Err.
	 *
	 * @param defaultValue The value to return in case of an Err.
	 * @return The success value or the default.
	 */
	T unwrapOr(T defaultValue);

	/**
	 * Maps an Ok value to a new value of type U.
	 * If the result is Err, the error is passed through.
	 *
	 * @param fn The function to apply to the success value.
	 * @return A new Result.
	 */
	<U> Result<U, E> map(Function<T, U> fn);

	/**
	 * The success case, holding a value of type T.
	 */
	record Ok<T, E>(T value) implements Result<T, E>
	{
		@Override
		public boolean isOk()
		{
			return true;
		}

		@Override
		public boolean isErr()
		{
			return false;
		}

		@Override
		public T unwrap()
		{
			return value;
		}

		@Override
		public E unwrapErr()
		{
			throw new RuntimeException("Called unwrapErr on Ok: " + value);
		}

		@Override
		public T unwrapOr(T defaultValue)
		{
			return value;
		}

		@Override
		public <U> Result<U, E> map(Function<T, U> fn)
		{
			return new Ok<>(fn.apply(value));
		}
	}

	/**
	 * The failure case, holding an error of type E.
	 */
	record Err<T, E>(E error) implements Result<T, E>
	{
		@Override
		public boolean isOk()
		{
			return false;
		}

		@Override
		public boolean isErr()
		{
			return true;
		}

		@Override
		public T unwrap()
		{
			throw new RuntimeException("Called unwrap on Err: " + error);
		}

		@Override
		public E unwrapErr()
		{
			return error;
		}

		@Override
		public T unwrapOr(T defaultValue)
		{
			return defaultValue;
		}

		@Override
		public <U> Result<U, E> map(Function<T, U> fn)
		{
			return new Err<>(error);
		}
	}
}