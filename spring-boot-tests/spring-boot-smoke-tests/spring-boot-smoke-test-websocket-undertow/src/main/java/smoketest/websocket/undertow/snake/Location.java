/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.websocket.undertow.snake;

/**
 * Location class.
 */
public class Location {

	/**
	 * The X location.
	 */
	public int x;

	/**
	 * The Y location.
	 */
	public int y;

	/**
     * Constructs a new Location object with the specified x and y coordinates.
     *
     * @param x the x coordinate of the location
     * @param y the y coordinate of the location
     */
    public Location(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
     * Returns the adjacent location in the specified direction.
     * 
     * @param direction the direction to move towards
     * @return the adjacent location in the specified direction
     */
    public Location getAdjacentLocation(Direction direction) {
		return switch (direction) {
			case NORTH -> new Location(this.x, this.y - SnakeUtils.GRID_SIZE);
			case SOUTH -> new Location(this.x, this.y + SnakeUtils.GRID_SIZE);
			case EAST -> new Location(this.x + SnakeUtils.GRID_SIZE, this.y);
			case WEST -> new Location(this.x - SnakeUtils.GRID_SIZE, this.y);
			case NONE -> this;
		};
	}

	/**
     * Compares this Location object to the specified object for equality.
     * 
     * @param o the object to compare to
     * @return true if the specified object is equal to this Location object, false otherwise
     */
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
		return this.y == location.y;
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the hashCode() method.
     *
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		int result = this.x;
		result = 31 * result + this.y;
		return result;
	}

}
