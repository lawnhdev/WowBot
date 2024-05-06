import java.util.LinkedList;
import java.util.Queue;

public class FixedSizeQueue {
    private Queue<Double> queue;
    private int maxSize;

    public FixedSizeQueue(int maxSize) {
        this.maxSize = maxSize;
        this.queue = new LinkedList<>();
    }

    public void add(double element) {
        if (queue.size() >= maxSize) {
            queue.poll(); // Remove the oldest element
        }
        queue.add(element);
    }

    public boolean checkIfPlayerIsStuck() {
        if (queue.isEmpty()) {
            return false; // Queue is empty
        }
        Double first = queue.peek(); // Get the first element
        Double last = ((LinkedList<Double>) queue).getLast(); // Get the last element

        return first.equals(last);
    }

    public Queue<Double> getQueue() {
        return queue;
    }
}