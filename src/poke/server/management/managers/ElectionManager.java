/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.management.managers;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

import poke.monitor.HeartMonitor;
import poke.monitor.MonitorHandler;
import poke.monitor.MonitorInitializer;
import poke.monitor.MonitorListener;
import poke.monitor.HeartMonitor.MonitorClosedListener;
import poke.server.Server;
import poke.server.conf.ServerConf;
import poke.server.management.ManagementQueue;
import eye.Comm.LeaderElection;
import eye.Comm.LeaderElection.VoteAction;
import eye.Comm.Management;
//Start Sweny Date: 03/18/2014
import poke.server.management.ManagementQueue.ManagementQueueEntry;
//End Sweny Date: 03/18/2014
/**
 * The election manager is used to determine leadership within the network.
 * 
 * @author gash
 * 
 */
public class ElectionManager {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionManager> instance = new AtomicReference<ElectionManager>();
	//Start Sweny
	protected ServerConf conf;
	protected String message;
	String myId;
	protected ChannelFuture channel;
	private MonitorHandler handler;
	private EventLoopGroup group;
	private static int N = 0;
	private String whoami;
	private String host;
	private int port;
	private List<MonitorListener> listeners = new ArrayList<MonitorListener>();
	//End Sweny
	//Atomic reference whose values are updated automatically. - Sweny
	private String nodeId;

	/** @brief the number of votes this server can cast */
	private int votes = 1;

	public static ElectionManager getInstance(String id, int votes) {
		instance.compareAndSet(null, new ElectionManager(id, votes));
		return instance.get();
	}

	public static ElectionManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the manager for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected ElectionManager(String nodeId, int votes) {
		this.nodeId = nodeId;

		if (votes >= 0)
			this.votes = votes;
	}
	/**
	 * @param sa 
	 * @param channel 
	 * @param args
	 */
	public void processRequest(LeaderElection req)  {

		if (req == null )
			return;
		//Start New Code -By Sweny Date: 03/12/2014
		ElectionManager electionMgr = ElectionManager.getInstance();
		if(conf != null){
			myId = conf.getServer().getProperty("node.id");
		}
		//End new Code by Sweny Date: 03/12/2014
		if (req.hasExpires()) {
			long ct = System.currentTimeMillis();
			if (ct > req.getExpires()) {
				// election is over
				return;
			}
		}

		if (req.getVote().getNumber() == VoteAction.ELECTION_VALUE) {
			// an election is declared!
			//Start New code Sweny Date: 03/09/2014
			if(!myId.isEmpty()){
				//String myId = conf.getServer().getProperty("node.id");
				//forward same id 
				electionMgr.sendElectionMsg(req.getNodeId(), VoteAction.ELECTION );			
			}
			//End New Code Sweny Date: 03/10/2014	
		} else if (req.getVote().getNumber() == VoteAction.DECLAREVOID_VALUE) {
			// no one was elected, I am dropping into standby mode`
		} else if (req.getVote().getNumber() == VoteAction.DECLAREWINNER_VALUE) {
			// some node declared themself the leader
			//Start new code Sweny - Date: 03/08/2014

			//End new code Sweny - Date: 03/08/2014
		} else if (req.getVote().getNumber() == VoteAction.ABSTAIN_VALUE) {
			// for some reason, I decline to vote
		} else if (req.getVote().getNumber() == VoteAction.NOMINATE_VALUE) {
			int comparedToMe = req.getNodeId().compareTo(nodeId);
			if (comparedToMe == -1) {
				// Someone else has a higher priority, forward nomination
				// TODO forward
				//Start new code Sweny - Date: 03/08/2014
				if(!myId.isEmpty()){
					electionMgr.sendElectionMsg(req.getNodeId(), VoteAction.ELECTION );
				}
				//End new code Sweny - Date: 03/08/2014
			} else if (comparedToMe == 1) {
				// I have a higher priority, nominate myself
				// TODO nominate myself
				//Start New Code : Sweny Date: 03/18/2014
				if(!myId.isEmpty()){
					electionMgr.sendElectionMsg(myId,VoteAction.ELECTION);
				}
			}else{
				electionMgr.sendElectionMsg(req.getNodeId(), VoteAction.DECLAREWINNER);
			}
		}
		//End New Code: Sweny Date:03/18/2014
	}

	//Start New Code Sweny Date:03/10/2014
	/**
	 * @param nodeId
	 * @param voteAction
	 */
	public void sendElectionMsg(String nodeId, VoteAction voteAction) {

		//LeaderElection.Builder leaderElect = LeaderElection.newBuilder().setNodeId(nodeId).setBallotId("BallotId").setDesc("ElectionMessage").setVote(voteAction);
		try {
			//Management.Builder req1 = Management.newBuilder().setElection(leaderElect.build());

			//Bootstrap b  =new Bootstrap().channel(ManagementQueueEntry);
			//ChannelPipeline p = Channels.newChannel(out);
			//Channel ch = connect();

			logger.info("sending election message");

			//Election.Builder n = Election.newBuilder();
			//ldr.setNodeId(nodeId);
			//Management.Builder m = Management.newBuilder();
			//m.setGraph(ldr.build());
			//channel.writeAndFlush(ldr.build());
			GeneratedMessage msg = generateElectionMessage();
			/*logger.info("-----------------before-------current channel of incoming "+HeartbeatManager.getInstance().incomingHB.values().toString());
			Iterator itr=  HeartbeatManager.getInstance().incomingHB.values().iterator();
			logger.info("itr ========="+itr.hasNext());
			if(itr.hasNext())
			{
				//HeartbeatData h=(HeartbeatData) itr.next();
				//logger.info("Testing         ------------"+ h.getChannel().toString());
				logger.info("Hellooo!!!!!!    "+((HeartbeatData) itr.next()).getChannel().toString());
				//HeartbeatData h=(HeartbeatData) itr.next();
				//logger.info("Testing         ------------"+ h.getChannel().toString());
				Map.Entry pairs=(Map.Entry)itr.next();
                Channel ch=(Channel) pairs.getKey();
                HeartbeatData hbd= (HeartbeatData)pairs.getValue();
                logger.info(" XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX from channel "+ch.toString()); 
                logger.info(" XXXXXXXXXXXXXXXXXXXXxx from heartbeat data "+ hbd.getChannel().toString());

			}

			for (HeartbeatData hd1 : HeartbeatManager.getInstance().incomingHB.values()){
				logger.info("---------------After----------------current channel of incoming "+ hd1.getChannel().toString());
			}*/

			logger.info( "Size of outgoingHB : " +HeartbeatManager.getInstance().outgoingHB.size()+ "HeartbeatManager.getInstance().outgoingHB.values(): "+HeartbeatManager.getInstance().outgoingHB.values());
			for(HeartbeatData hb : HeartbeatManager.getInstance().outgoingHB.values())
				//for(Channel ch : HeartbeatManager.getInstance().outgoingHB.keySet())
			{
				logger.info("HB data!"+hb.channel.localAddress().toString());
				//
				//logger.info("-------------------------------------current channel calues "+ch.localAddress().toString());
				if (hb.channel.isOpen()) {
					logger.info("Channel is open!");
					if (hb.channel.isWritable()){
						hb.channel.flush();
						hb.channel.writeAndFlush(msg);
						logger.info("Message send successfully!");
						logger.info("Value of Channel: "+hb.getChannel().toString());
						ManagementQueue.enqueueResponse((Management) msg, hb.channel);
					}
				}else{
					logger.info("Channel not open!");
				}
				logger.info("leader election message sent");
				//return leaderElect.build();
			}
		} catch (Exception e) {
			// logger.error("could not send connect to node", e);
		}
	}

	private Management generateElectionMessage() {

		LeaderElection.Builder lead= LeaderElection.newBuilder();

		lead.setNodeId(nodeId);
		lead.setBallotId("test");
		lead.setVote(VoteAction.ELECTION);
		lead.setDesc("Election Start");


		Management.Builder b = Management.newBuilder();

		b.setElection(lead.build());
		return b.build();
	}

	/*protected Channel connect() {
	// Start the connection attempt.

	if (channel == null) {
		try {
			handler = new MonitorHandler();
			MonitorInitializer mi = new MonitorInitializer(handler, false);

			Bootstrap b = new Bootstrap();
			// @TODO newFixedThreadPool(2);
			b.group(group).channel(NioSocketChannel.class).handler(mi);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(host, port).syncUninterruptibly();
			channel.awaitUninterruptibly(5000l);
			channel.channel().closeFuture().addListener(new MonitorClosedListener(this));

			if (N == Integer.MAX_VALUE)
				N = 1;
			else
				N++;

			// add listeners waiting to be added
			if (listeners.size() > 0) {
				for (MonitorListener ml : listeners)
					handler.addListener(ml);
				listeners.clear();
			}
		} catch (Exception ex) {
			logger.debug("failed to initialize the heartbeat connection");
			// logger.error("failed to initialize the heartbeat connection",
			// ex);
		}
	}*/

	//End New Code Sweny Date:03/10/2014
}
