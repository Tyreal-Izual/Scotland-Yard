package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {
	public Integer alpha=-100,beta=200;
	public List<Piece>detectives=new ArrayList<>();
	@Nonnull @Override public String name() { return "GDY"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		alpha = -100; /** minmax Upper and lower bounds */
		beta = 400;
		var moves = board.getAvailableMoves().asList();
		var P = board.getPlayers();


		HashMap<Integer, Integer> scores = new HashMap<>();  /** Add a new set and put scores into it  */
		detectives = getdetectives(board.getPlayers().asList());  /** Three values Given in my game state */
		Board.GameState aa = (Board.GameState) board;
		Integer count = 0, maxScore = -100;
		System.out.println("mrx moves:  " + moves);
		System.out.println("detectives moves:" + aa.advance(moves.get(0)).getAvailableMoves());
		for (Move move : moves) {

			var newstate = aa.advance(move);
			Integer s;

			if (alpha >= beta) {
				break;
			} else {
				s = getscore1(newstate, alpha, beta, move, 0, moves);
				scores.put(s, count);
				if (s > maxScore) {
					maxScore = s;
					alpha = maxScore;
				}
				count++;
			}
		}
		Optional<Integer> max = scores.keySet().stream().max(Integer::compareTo);
		return moves.get(scores.get(max.get()));
	}


	private Integer getscore1(Board.GameState state, Integer alpha, Integer beta, Move oldmove, Integer count, ImmutableList<Move> oldmoves){
		if(!state.getWinner().isEmpty()){
			if(state.getWinner().contains(Piece.MrX.MRX)){
			return score(getDL(state),getML(oldmove),state.getSetup().graph)+additionalscore(oldmoves)+200;

			}else {
				return 0;
			}
		}else if(count==7){/**number of depth，7-9 best, >9 time limit */
		return score(getDL(state),getML(oldmove),state.getSetup().graph)+additionalscore(oldmoves);

		}

     	var moves = state.getAvailableMoves().asList();
		if(moves.get(0).commencedBy()==Piece.MrX.MRX){
			Integer maxScore=-100;

			for(Move move:moves){
				var newstate=state.advance(move);
				var c=count+1;
				var s=getscore1(newstate,alpha,beta,move,c,moves);

				if(s>maxScore){
					maxScore=s;
					alpha=maxScore;
				}
				if(alpha>=beta){
					break;
				}
			}
			return maxScore;
		}else {
			Integer minScore=1000;

			for(Move move:moves){
				if(moves.get(0).commencedBy()==move.commencedBy()) {
					var newstate = state.advance(move);
					var c = count + 1;
					var s = getscore1(newstate, alpha, beta, oldmove, c, oldmoves);
					if (s < minScore) {
						minScore = s;
						beta = minScore;
					}
					if (alpha >= beta) {

						break;
					}
				}
			}
			return minScore;
		}
	}

	private Integer additionalscore(ImmutableList<Move> moves) { /**additionalscore: Calculate the impact of the next step */
		Integer count=0;
		Integer score=0;
		for (Move move:moves){
			Integer s=move.accept(new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					switch (move.ticket){
						case TAXI : return 4;
						case BUS : return 8;
						case UNDERGROUND: return 15;
						case SECRET: return 30;
					}
					return 0;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {/**上来会用，所以调的大*/
						return 100;
				}
			});
			if (s==50&&count==0){
				count++;
				score+=s;
			}else if (s!=50){
				score+=s;
			}
		}
		return score;
	}

	private List<Piece> getdetectives(ImmutableList<Piece> pieces){ /**detective information*/
		List<Piece> P=new ArrayList<>();
		for(Piece p:pieces){
			if(p!=Piece.MrX.MRX){
				P.add(p);
			}
		}
		return P;
	}

	private Integer getML(Move move) {/**Mr.X Location*/
		Integer MLocation=move.accept(new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});
		return MLocation;
	}

	private List<Integer> getDL(Board.GameState board){ /**detective location*/
		var P=board.getPlayers();
		List<Integer> DLocations =new ArrayList<>();

		for(Piece p:P){
			if(p.isDetective()){
				DLocations.add(board.getDetectiveLocation((Piece.Detective) p).get());
			}
		}
		return DLocations;
	}


	/**1.score Find the maximum distance between all detectives and Mr.X（Keep Mr.x furthest from detectives）*/

	private Integer score(List<Integer> DLocations/** Detective */,Integer MLocation/** Mr.X origin point */, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
		HashMap<Integer/** point */,Integer/** distance */> v=new HashMap<>();  /**The first part written with Dijkstra for check paths */
		List<Integer> s=new ArrayList<>(); /** Initialize and store the point where the shortest path has been found */
		List<Integer> unsettled=new ArrayList<>();/** point data not found */
		v.put(MLocation,0);/** Let Mlocation(Mr.X) is 0 */
		s.add(MLocation);/** Put found location in to MRXlocationadd */
		Integer count=0,score=0;/** count(round)，score(final score) */
		Integer Fmin=1000;/** specific size */
		List<Integer> minK/** Minimum value interval（List is used because there may be the same value）*/=new ArrayList<>();/** We calculate the shortest path from detective to Mrx */
		while(count<s.size()){
			Integer current=s.get(count);
			for(Integer destination:graph.adjacentNodes(current)){/** Find adjacent points */
				if(!v.containsKey(destination)){
					v.put(destination,getValue(destination,current,graph)+v.get(current));
					unsettled.add(destination);
				}else if(v.get(destination)>getValue(destination,current,graph)+v.get(current)){
					v.replace(destination,getValue(destination,current,graph)+v.get(current));
				}
			}
			minK.clear();
			boolean changed=false;
			for(Integer k:unsettled){
				if(v.get(k)==Fmin){
					minK.add(k);
				}
				if(v.get(k)<Fmin){
					Fmin=v.get(k);
					minK.clear();
					minK.add(k);
					changed=true;
				}
			}
			if(changed==true){
			Fmin=1000;
			for(Integer K:minK){
				s.add(K);
				unsettled.remove(K);
			}
			}
			count++;
			if(s.containsAll(DLocations)){
				for(Integer l: DLocations){
					score=score+v.get(l);
				}
				break;  /** Add up the distance from all detectives to Mrx to get this score */
				/** Calculate the detective distance of the next step and find the longest one */
			}
		}
		return score;
	}
	private Integer getValue(Integer destination,Integer source,ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){ /** Sort all cards from small to large(secret or double can run the farthest once and taxi can run the shortest once) */
		Integer value=0;
		for(ScotlandYard.Transport T:graph.edgeValueOrDefault(source,destination,ImmutableSet.of())){
			switch (T.requiredTicket()){
				case TAXI: value=value+1; break;/** Node to node scoring */
				case BUS:value=value+2; break;
				case UNDERGROUND: value=value+3; break;
				case SECRET:value=value+4; break;
			}
		}
		return value;
	}
}
