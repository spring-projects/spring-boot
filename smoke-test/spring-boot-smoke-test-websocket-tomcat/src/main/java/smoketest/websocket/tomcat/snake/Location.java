/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.tomcat.snake;

import org.jspecify.annotations.Nullable;

public class Location {

	/**
	 * The X location.
	 */
	public int x;

	/**
	 * The Y location.
	 */
	public int y;

	public Location(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Location getAdjacentLocation(Direction direction) {
		return switch (direction) {
			case NORTH -> new Location(this.x, this.y - SnakeUtils.GRID_SIZE);
			case SOUTH -> new Location(this.x, this.y + SnakeUtils.GRID_SIZE);
			case EAST -> new Location(this.x + SnakeUtils.GRID_SIZE, this.y);
			case WEST -> new Location(this.x - SnakeUtils.GRID_SIZE, this.y);
			case NONE -> this;
		};
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Location location = (Location) o;
		if (this.x != location.x) {
			return false;
		}
		return this.y == location.y;
	}

	@Override
	public int hashCode() {
		int result = this.x;
		result = 31 * result + this.y;
		return result;
	}

}
