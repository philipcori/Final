import java.util.ArrayList;

import shiffman.box2d.*;

import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.DistanceJointDef;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import javax.swing.JOptionPane;

public class QuickTest extends PApplet {

	// A reference to our box2d world
	Box2DProcessing box2d;

	ArrayList<Boundary> boundaries;
	ArrayList<Box> boxes;

	Spring spring;
	int goalHeight;
	int passingCount;
	PImage background;
	int level = 1;
	boolean levelOver = false;
	int difficulty;
	int timeLimit;
	boolean gameOver = false;

	public void setup() {
		size(600, 600);
		smooth();

		// Initialize box2d physics and create the world
		box2d = new Box2DProcessing(this, 10);
		box2d.createWorld();
		// We are setting a custom gravity
		box2d.setGravity(0, -40);

		// Create ArrayLists
		boxes = new ArrayList<Box>();
		boundaries = new ArrayList<Boundary>();
		// Add a bunch of fixed boundaries
		createBoundaries();

		String response = JOptionPane.showInputDialog("Enter a difficulty 1-5");
		difficulty = parseInt(response);
		createBoxes(difficulty);

		spring = new Spring();

		passingCount = 0;
		background = loadImage("../resources/citybackground.png");
		background.resize(600, 600);

		goalHeight = 200;
		timeLimit = 2000;

	}

	public void draw() {
		background(background);
		// We must always step through time!
		box2d.step(1.0f / 60, 10, 10);
		spring.update(mouseX, mouseY);

		fill(255, 0, 0);
		rect(0, height - goalHeight, 2 * width, 2);
		for (Boundary wall : boundaries) {
			wall.display();
		}
		for (Box b : boxes) {
			b.display();
		}
		spring.display();

		String msg = "Time left: " + (timeLimit - frameCount);
		if (goalHeight == height)
			msg = "You beat the game!";
		if ((timeLimit - frameCount < 0)) { // if you ran out of time
			exit();
		}
		// so text isn't covered by boxes in last level.
		if (level != 5) {
			text(msg, 10, 50);
			text("Level " + level, 10, 70);
		} else {
			text(msg, 10, 350);
			text("Final level! Build downwards!", 10, 370);
		}

		if (frameCount > 250 && reachedGoal()) { // makes sure level doesn't beat itself
			passingCount++;
		} else
			passingCount = 0;
		if (passingCount > 110) { // if you've been at the goalHeight for 110 frames
			reset(level);
			level++;

		}

	}

	// classes we used that were already given
	class Boundary {

		// A boundary is a simple rectangle with x,y,width,and height
		float x;
		float y;
		float w;
		float h;

		// But we also have to make a body for box2d to know about it
		Body b;

		Boundary(float x_, float y_, float w_, float h_) {
			x = x_;
			y = y_;
			w = w_;
			h = h_;

			// Create the body
			BodyDef bd = new BodyDef();
			bd.position.set(box2d.coordPixelsToWorld(x, y));
			b = box2d.createBody(bd);

			// Figure out the box2d coordinates
			float box2dW = box2d.scalarPixelsToWorld(w / 2);
			float box2dH = box2d.scalarPixelsToWorld(h / 2);

			// Define the polygon
			PolygonShape sd = new PolygonShape();
			sd.setAsBox(box2dW, box2dH);

			FixtureDef fd = new FixtureDef();
			fd.shape = sd;
			fd.density = 0;
			fd.friction = 0.3f;
			fd.restitution = 0.5f;

			b.createFixture(fd);

			ChainShape cs;
			// cs.create

		}

		// Draw the boundary, if it were at an angle we'd have to do something
		// fancier
		public void display() {
			fill(0);
			stroke(0);
			rectMode(CENTER);
			rect(x, y, w, h);
		}

	}

	class Box {

		// We need to keep track of a Body and a width and height
		Body body;
		float w;
		float h;

		// Constructor
		Box(float x, float y, float w, float h) {
			this.w = w;
			this.h = h;
			// Add the box to the box2d world
			makeBody(new Vec2(x, y), w, h);
		}

		// This function removes the particle from the box2d world
		public void killBody() {
			Fixture f = body.getFixtureList();
			// println(f);

			box2d.destroyBody(body);
		}

		// Is the particle ready for deletion?
		public boolean done() {
			// Let's find the screen position of the particle
			Vec2 pos = box2d.getBodyPixelCoord(body);
			// Is it off the bottom of the screen?
			if (pos.y > height + w * h) {
				killBody();
				return true;
			}
			return false;
		}

		// Drawing the box
		public void display() {
			// We look at each body and get its screen position
			Vec2 pos = box2d.getBodyPixelCoord(body);
			// Get its angle of rotation
			float a = body.getAngle();

			rectMode(CENTER);
			pushMatrix();
			translate(pos.x, pos.y);
			rotate(-a);

			fill(255);
			stroke(0);
			rect(0, 0, w, h);
			popMatrix();
		}

		// This function adds the rectangle to the box2d world
		public void makeBody(Vec2 center, float w_, float h_) {

			// Define a polygon (this is what we use for a rectangle)
			PolygonShape sd = new PolygonShape();
			float box2dW = box2d.scalarPixelsToWorld(w_ / 2);
			float box2dH = box2d.scalarPixelsToWorld(h_ / 2);
			sd.setAsBox(box2dW, box2dH);

			// Define a fixture
			FixtureDef fd = new FixtureDef();
			fd.shape = sd;
			// Parameters that affect physics
			fd.density = 1;
			fd.friction = 0.3f;
			fd.restitution = 0.5f;

			// Define the body and make it from the shape
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			bd.position.set(box2d.coordPixelsToWorld(center));

			CircleShape cs = new CircleShape();

			DistanceJointDef djd;

			body = box2d.createBody(bd);
			body.createFixture(fd);
			// body.setMassFromShapes();

			// Give it some initial random velocity
			body.setLinearVelocity(new Vec2(random(-5, 5), random(2, 5)));
			body.setAngularVelocity(random(-5, 5));
		}

		boolean contains(float x, float y) {
			Vec2 worldPoint = box2d.coordPixelsToWorld(x, y);
			Fixture f = body.getFixtureList();
			boolean inside = f.testPoint(worldPoint);
			return inside;
		}
	}

	class Spring {

		// This is the box2d object we need to create
		MouseJoint mouseJoint;

		Spring() {
			// At first it doesn't exist
			mouseJoint = null;
		}

		// If it exists we set its target to the mouse location
		void update(float x, float y) {
			if (mouseJoint != null) {
				// Always convert to world coordinates!
				Vec2 mouseWorld = box2d.coordPixelsToWorld(x, y);
				mouseJoint.setTarget(mouseWorld);
			}
		}

		void display() {
			if (mouseJoint != null) {
				// We can get the two anchor points
				Vec2 v1 = new Vec2(0, 0);
				mouseJoint.getAnchorA(v1);
				Vec2 v2 = new Vec2(0, 0);
				mouseJoint.getAnchorB(v2);
				// Convert them to screen coordinates
				v1 = box2d.coordWorldToPixels(v1);
				v2 = box2d.coordWorldToPixels(v2);
				// And just draw a line
				stroke(0);
				strokeWeight(1);
				line(v1.x, v1.y, v2.x, v2.y);
			}
		}

		// This is the key function where
		// we attach the spring to an x,y location
		// and the Box object's location
		void bind(float x, float y, Box box) {
			// Define the joint
			MouseJointDef md = new MouseJointDef();

			// Body A is just a fake ground body for simplicity (there isn't
			// anything at the mouse)
			md.bodyA = box2d.getGroundBody();
			// Body 2 is the box's boxy
			md.bodyB = box.body;
			// Get the mouse location in world coordinates
			Vec2 mp = box2d.coordPixelsToWorld(x, y);
			// And that's the target
			md.target.set(mp);
			// Some stuff about how strong and bouncy the spring should be
			md.maxForce = (float) 1000.0 * box.body.m_mass;
			md.frequencyHz = (float) 5.0;
			md.dampingRatio = (float) 0.9;

			// Wake up body!
			// box.body.wakeUp();

			// Make the joint!
			mouseJoint = (MouseJoint) box2d.world.createJoint(md);
		}

		void destroy() {
			// We can get rid of the joint when the mouse is released
			if (mouseJoint != null) {
				box2d.world.destroyJoint(mouseJoint);
				mouseJoint = null;
			}
		}

	}


	public void mousePressed() {

		for (Box b : boxes) {
			if (b.contains(mouseX, mouseY)) {
				// check if mouse is inside box
				spring.bind(mouseX, mouseY, b);
			}
		}
	}

	public void mouseReleased() {
		spring.destroy();
	}

	public boolean reachedGoal() {
		if (spring.mouseJoint != null)
			return false;
		// return false if your grabbing something
		for (int c = 0; c < width; c++) {
			for (Box b : boxes) {
				if (b.contains(c, height - goalHeight)) {
					return true;
				}
				// if any box is along the line, return true;
			}
		}
		return false;
	}

	public void reset(int level) {
		// makes new boxes, increases height, reverses gravity on last level,
		// exits if you beat the game
		frameCount = 0;
		passingCount = 0;
		levelOver = false;
		goalHeight += 100;
		for (Box b : boxes) {
			b.killBody();
		}
		for (int i = boxes.size() - 1; i >= 0; i--) {
			boxes.remove(i);
		}
		createBoxes(difficulty);

		if (goalHeight == height) {
			goalHeight = 200;
			box2d.setGravity(0, 40);
			text("Build downward!!", 10, 70);
		}
		if (level == 5) {
			exit();
		}

	}

	public void createBoxes(int difficulty) {
		float w, h;
		for (int i = 0; i < 10; i++) {
			// make width && height random, but neither less than 20
			do {
				h = (float) (Math.random() * 100 + 1);
				w = (float) (Math.random() * 100 + 1);
			} while (h < 20 || w < 20);
			w /= (difficulty * 0.8); // make more skinny based on difficulty
			boxes.add(new Box((int) (Math.random() * (width - 100)),
					(int) (Math.random() * (height / 2) + 200), w, h));
			// spawn box at random coordinate
		}
	}

	public void createBoundaries() {
		boundaries.add(new Boundary(0, height, width * 2, 10));
		boundaries.add(new Boundary(0, 0, width * 2, 10));
		boundaries.add(new Boundary(0, 0, 10, height * 2));
		boundaries.add(new Boundary(width - 5, 0, 10, height * 2));
		// adds boundaries on all sides
	}

	static public void main(String args[]) {
		PApplet.main(new String[] { "QuickTest" });
	}

}
