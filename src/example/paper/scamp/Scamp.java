package example.paper.scamp;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

import javax.swing.plaf.synth.SynthEditorPaneUI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by julian on 3/14/15.
 */
public class Scamp implements Linkable, EDProtocol, CDProtocol, example.PeerSamplingService {

    private static final String PAR_C = "c";
    private static final String SCAMP_PROT = "0";
    private static final String PAR_TRANSPORT = "transport";
    private static final String PAR_LEASE_MAX = "leaseTimeoutMax";
    private static final String PAR_LEASE_MIN = "leaseTimeoutMin";


    // ===========================================
    // P R O P E R T I E S
    // ===========================================

    public View in;
    public View out;
    private View.ViewEntry current;

    public static int pid;
    public static int tid;
    public static int c;
    public final int MAX_LEASE;
    public final int MIN_LEASE;

    // ===========================================
    // C T O R
    // ===========================================

    public Scamp(String n) {
        this.tid = Configuration.getPid(n + "." + PAR_TRANSPORT);
        this.c = Configuration.getInt(n + "." + PAR_C, 0);
        this.pid = Configuration.lookupPid(SCAMP_PROT);
        this.MAX_LEASE = Configuration.getInt(n + "." + PAR_LEASE_MAX, 2000);
        this.MIN_LEASE = Configuration.getInt(n + "." + PAR_LEASE_MIN, 1000);
        this.in = new View(this.MIN_LEASE, this.MAX_LEASE);
        this.out = new View(this.MIN_LEASE, this.MAX_LEASE);
    }

    @Override
    public Object clone() {
        Scamp scamp = null;
        try {
            scamp = (Scamp) super.clone();
            scamp.in = new View(this.MIN_LEASE, this.MAX_LEASE);
            scamp.out = new View(this.MIN_LEASE, this.MAX_LEASE);
        } catch (CloneNotSupportedException e) {
        } // never happens
        // ...
        return scamp;
    }

    // ===========================================
    // P U B L I C
    // ===========================================

    @Override
    public void nextCycle(Node node, int protocolID) {

        this.out.updateTimeouts();

        if (this.current != null && this.current.timeout()) {
            this.onRejoin(node);
        }
    }

    @Override
    public int degree() {
        return this.out.size();
    }

    @Override
    public Node getNeighbor(int i) {
        return this.out.array.get(i).node;
    }

    @Override
    public boolean addNeighbor(Node neighbour) {
        throw new RuntimeException("nope..");
        //return false;
    }

    public boolean contains(View.ViewEntry ve) {
        return this.out.contains(ve);
    }

    @Override
    public boolean contains(Node neighbor) {
        return this.out.contains(neighbor);
    }

    @Override
    public void pack() {

    }

    @Override
    public void onKill() {

    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        ScampMessage message = (ScampMessage) event;

        switch (message.type) {
            case ForwardSubscription:
                this.onForward(node, message);
                break;
            case Subscribe:
            case Resubscribe:
                this.onSubscribe(node, message);
                break;
            case Accepted:
                this.in.add(message.sender); // here we dont care about timeout
                break;
            default:
                throw new RuntimeException("unhandled event");
        }

    }

    @Override
    public List<Node> getPeers() {
        return this.out.list();
    }

    @Override
    public String debug() {
        return "{in:" + this.in + ", out:" + this.out + "}";
    }

    @Override
    public int callsInThisCycle() {
        return 0;
    }

    @Override
    public void clearCallsInCycle() {

    }

    /**
     * send a message to the network
     *
     * @param destination
     * @param message
     */
    public void send(Node me, Node destination, ScampMessage message) {
        final Node sender = message.sender;
        if (sender.getID() == destination.getID()) {
            System.err.println("@:" + me.getID());
            System.err.println("in:" + this.in);
            System.err.println("out:" + this.out);
            throw new RuntimeException("must not send to oneself -->" +
                    destination.getID() + " | " + message);
        }
        Transport tr = (Transport) sender.getProtocol(tid);
        tr.send(sender, destination, message, pid);
    }

    // ===========================================
    // P R O T O C O L
    // ===========================================

    /**
     * initializes the very first node
     *
     * @param me
     */
    public static void initialize(Node me) {
        Scamp prot = (Scamp) me.getProtocol(pid);
        prot.current = View.generate(me);
    }

    /**
     * Joins the node to the network
     *
     * @param me  the node that is supposed to join
     * @param max the maximum value from which we can select items for indirection
     */
    public static void subscribe(Node me, int max) {
        Scamp prot = (Scamp) me.getProtocol(pid);
        prot.current = View.generate(me);

        Node other = Network.get(0);
        // Indirection
        if (max > 0) {
            other = Network.get(CommonState.r.nextInt(max));
        }
        Scamp otherProt = (Scamp) other.getProtocol(pid);

        if (other.getID() == me.getID()) {
            throw new RuntimeException("Lol nope!");
        }

        prot.out.add(otherProt.current);
        ScampMessage subscribe = ScampMessage.subscribe(prot.current);
        prot.send(me, other, subscribe);
    }

    /**
     * get called upon receiving a subscription
     *
     * @param me
     * @param subscription
     */
    private void onSubscribe(Node me, ScampMessage subscription) {
        if (subscription.type != ScampMessage.Type.Subscribe &&
                subscription.type != ScampMessage.Type.Resubscribe) {
            throw new RuntimeException("wrong message type");
        }
        this.out.updateTimeouts();
        final boolean resubscribe = subscription.type == ScampMessage.Type.Resubscribe;

        for (Node n : this.out.list()) {
            final ScampMessage forward = ScampMessage.forward(me, subscription);
            this.send(me, n, forward);
        }

        if (!resubscribe) {
            // only do this when actually joining the network (otherwise we would introduce too many arcs)
            for (int i = 0; i < c && this.out.size() > 0; i++) {
                final ScampMessage forward = ScampMessage.forward(me, subscription);
                final Node destination = this.out.getRandom();
                this.send(me, destination, forward);
            }
        }
    }

    /**
     * gets called upon forwarding
     *
     * @param me
     * @param forwarded
     */
    public void onForward(Node me, ScampMessage forwarded) {
        if (forwarded.type != ScampMessage.Type.ForwardSubscription) {
            throw new RuntimeException("wrong message type");
        }

        this.out.updateTimeouts();
        if (!this.contains(forwarded.subscriber) && p() && forwarded.subscriber.node.getID() != me.getID()) {
            this.out.add(forwarded.subscriber);
            final ScampMessage accept = ScampMessage.accepted(me);
            this.send(me, forwarded.subscriber.node, accept);
        } else if (forwarded.ttl > 0) {
            if (this.out.size() > 0) {
                Node destination = this.out.getRandom();

                if (destination.getID() == forwarded.subscriber.node.getID()) {
                    if (this.out.size() > 1) {
                        while (destination.getID() == forwarded.subscriber.node.getID()) {
                            destination = this.out.getRandom();
                        }
                        this.send(me, destination, ScampMessage.forward(me, forwarded));
                    }
                } else {
                    this.send(me, destination, ScampMessage.forward(me, forwarded));
                }
            } else {
                System.err.println("Forwarding is starving..");
            }
        }
    }

    /**
     * LEASE mechanics
     *
     * @param me
     */
    public void onRejoin(Node me) {
        // create a new frame
        this.current = View.generate(me);

        if (this.out.size() > 0) {
            //this.in.clear();
            Node contact = this.out.getRandom();
            final ScampMessage resubscribe = ScampMessage.resubscribe(this.current);
            this.send(me, contact, resubscribe);
        } else { // this only happens when errors are introduced..
            System.err.println("COULD NOT RESUBSCRIBE");
        }
    }

    @Override
    public String toString() {
        return this.debug();
    }

    // ===========================================
    // P R I V A T E
    // ===========================================

    private boolean p() {
        return CommonState.r.nextDouble() < 1.0 / (1.0 + this.degree());
    }

}
