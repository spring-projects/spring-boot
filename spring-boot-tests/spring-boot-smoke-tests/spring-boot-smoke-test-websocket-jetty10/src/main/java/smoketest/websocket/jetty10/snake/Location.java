/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.websocket.jetty10.snake;

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
		switch (direction) {
		case NORTH:
			return new Location(this.x, this.y - SnakeUtils.GRID_SIZE);
		case SOUTH:
			return new Location(this.x, this.y + SnakeUtils.GRID_SIZE);
		case EAST:
			return new Location(this.x + SnakeUtils.GRID_SIZE, this.y);
		case WEST:
			return new Location(this.x - SnakeUtils.GRID_SIZE, this.y);
		case NONE:
			// fall through
		default:
			return this;
		}
	}

	@Override
	public boolean equals(Object o) {
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
		if (this.y != location.y) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = this.x;
		result = 31 * result + this.y;
		return result;
	}

}
