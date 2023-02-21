package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import java.util.List;
import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private ImmutableSet<Piece> everyone;
		private ImmutableList<Piece> remainingRounds;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if (remaining.isEmpty()) throw new IllegalArgumentException("Remaining is empty!");
			if (mrX.isDetective()) throw new IllegalArgumentException("mrX is empty!");
			if (mrX == null) throw new IllegalArgumentException("mrX is null");
			if (detectives.isEmpty()) throw new IllegalArgumentException("detectives is empty!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("empty graph!");


			for(Player p :detectives){
				if(p.has(Ticket.DOUBLE)) throw new IllegalArgumentException("detective has double ticket");
			}
			for(Player p :detectives){
				if(p.has(Ticket.SECRET)) throw new IllegalArgumentException("detective has secret ticket");
			}
			for(int i=0;i<detectives.size();i++){
				for(int j=i+1;j<detectives.size();j++){
					if(detectives.get(i).location()==detectives.get(j).location()) throw new IllegalArgumentException("location duplicated!");
				}
			}



			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner=ImmutableSet.of();

			if (getWinner().isEmpty()&&getAvailableMoves().isEmpty()){
				if(this.remaining.contains(MrX.MRX)) {
					List<Piece> p = new ArrayList<>();
					this.detectives.forEach(x -> p.add(x.piece()));
					this.remaining = ImmutableSet.copyOf(p);
				}
			else{
					this.remaining=ImmutableSet.of(MrX.MRX);
				}
			}
			System.out.println(this.remaining);


		}

		/**Getters*/
		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<>();
			players.add(this.mrX.piece());
			for (Player p : this.detectives)
				players.add(p.piece());
			return ImmutableSet.copyOf(players);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player p : this.detectives)
				if (p.piece() == detective)
					return Optional.of(p.location());
			return Optional.empty();
		}


		/**
		 *
		 * @param piece the player piece
		 * @return
		 */
		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {

			class MyTicketBoard implements TicketBoard {

				final Player player;

				MyTicketBoard(Player player) {
					this.player = player;
				}

				public int getCount(@Nonnull Ticket ticket) {
					return this.player.tickets().get(ticket);
				}
			}

			for (Player p : this.detectives) {
				if (p.piece() == piece) {
					return Optional.of(new MyTicketBoard(p));
				}
			}

				if (piece == this.mrX.piece()) {
					return Optional.of(new MyTicketBoard(this.mrX));
				}
				return Optional.empty();
			}


			/**
			 * MrX TravelLog
			 *
			 * @return
			 */
			@Nonnull
			@Override
			public ImmutableList<LogEntry> getMrXTravelLog () {
				return log;
			}

			/**
			 * get Winner
			 *
			 * @return
			 */
			@Nonnull
			@Override
			public ImmutableSet<Piece> getWinner () {

				List<Piece> winner = new ArrayList<>();
				ImmutableSet<Piece> bufferRemaining = this.remaining;
				List<Piece> detectivePieces = new ArrayList<>();
				this.detectives.forEach(x -> detectivePieces.add(x.piece()));

				/** Detective Winning Scenarios */
				for (Player detective : this.detectives) { /** mrX is captured*/
					if (detective.location() == this.mrX.location()) {
						winner = detectivePieces;
						this.winner = ImmutableSet.copyOf(winner);
						return ImmutableSet.copyOf(winner);
						//break;
					}
				}

				/** MrX Winning Scenarios */
				if (log.size()==setup.moves.size()&&bufferRemaining.contains(MrX.MRX)) { /**there are no more remaining rounds*/
					winner.add(this.mrX.piece());
					this.winner=ImmutableSet.copyOf(winner);
					return ImmutableSet.copyOf(winner);
				}
				this.remaining = ImmutableSet.copyOf(detectivePieces);
				if (getAvailableMoves().isEmpty()&&!bufferRemaining.containsAll(this.detectives)) { /**if detectives are out of tickets or stuck*/
					winner.add(this.mrX.piece());
					this.remaining = bufferRemaining;
					this.winner=ImmutableSet.copyOf(winner);
					return ImmutableSet.copyOf(winner);
				}
				this.remaining = bufferRemaining;

				/**Detective Winning Scenarios*/

				this.remaining = ImmutableSet.of(this.mrX.piece());
				if (this.getAvailableMoves().isEmpty()&&bufferRemaining.contains(MrX.MRX)) { /** mrX is stuck or has no tickets*/
					winner = detectivePieces;

				}
				this.remaining = bufferRemaining;
				this.winner=ImmutableSet.copyOf(winner);

				return ImmutableSet.copyOf(winner);


			}

			/**
			 * singleMove
			 *
			 * @param setup
			 * @param detectives
			 * @param player
			 * @param source
			 * @return
			 */
			private ImmutableSet<SingleMove> getSingleMoves (
					GameSetup setup,
					List < Player > detectives,
					Player player,
					int source){
				var singleMoves = new ArrayList<SingleMove>();
				ArrayList<Integer> detectiveLocations = new ArrayList<>();

				for (Player p : this.detectives) {
					detectiveLocations.add(p.location());
				}

				for (int destination : setup.graph.adjacentNodes(source)) {

					if (!(detectiveLocations.contains(destination))) {

						for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
							if (player.has(t.requiredTicket())) {
								singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
							}
						}

						if (player.has(Ticket.SECRET)) {
							singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
						}
					}
				}
				return ImmutableSet.copyOf(singleMoves);
			}

			/**
			 * DoubleMove
			 *
			 * @param setup
			 * @param detectives
			 * @param player
			 * @param source
			 * @return
			 */
			private ImmutableSet<DoubleMove> getDoubleMoves (
					GameSetup setup,
					List < Player > detectives,
					Player player,
					int source){

				var doubleMoves = new ArrayList<DoubleMove>();
				final var firstSingleMoves = getSingleMoves(setup, detectives, player, source);

				if (player.has(Ticket.DOUBLE) && setup.moves.size()-log.size() > 1) { /**this.remainingRounds.size() > 1 (old 4/9) */



					for (SingleMove sMove1 : firstSingleMoves) {

						var secondSingleMoves = getSingleMoves(setup, detectives, player, sMove1.destination);


						for (SingleMove sMove2 : secondSingleMoves) {

							if (sMove1.ticket != sMove2.ticket || player.hasAtLeast(sMove1.ticket, 2)) {
								DoubleMove doubleMove = new DoubleMove(
										player.piece(),
										sMove1.source(),
										sMove1.ticket,
										sMove1.destination,
										sMove2.ticket,
										sMove2.destination
								);

								doubleMoves.add(doubleMove);
							}
						}
					}
				}

				return ImmutableSet.copyOf(doubleMoves);
			}

			@Nonnull
			@Override
			public ImmutableSet<Move> getAvailableMoves () {
				if(!this.winner.isEmpty())
					return ImmutableSet.of();
				List<Move> moves = new ArrayList<>();
				if (this.remaining.contains(this.mrX.piece())) { /** get mrX moves */
					ImmutableSet<SingleMove> sMoves = getSingleMoves(setup, detectives, mrX, mrX.location());
					List<DoubleMove> dMoves = List.copyOf(getDoubleMoves(
							this.setup,
							this.detectives,
							mrX,
							mrX.location()));
					moves.addAll(sMoves);
					moves.addAll(dMoves);
				} else {  /** get detective moves */
					for (Player p : this.detectives) {
						if (this.remaining.contains(p.piece())) {
							ImmutableSet<SingleMove> sMoves = getSingleMoves(setup, detectives, p, p.location());
							moves.addAll(sMoves);
						}
					}
				}
				System.out.println(moves);
				return ImmutableSet.copyOf(moves);
			}


			/**
			 * advanced move
			 *
			 * @param move the move to make
			 * @return
			 */
			@Nonnull
			@Override
			public GameState advance (Move move){
				this.moves = this.getAvailableMoves();
				if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
				//system.out.println(move.tickets())

				/**
				List<LogEntry> logs = new ArrayList<>();  Convert list to new Array list
				*/
				ImmutableList<LogEntry> newlog;

				int newDestination;

				List<LogEntry> l=new ArrayList<>(log);
				List<Piece> newRemaining=new ArrayList<>();
				List<Integer> MDestination=move.accept(new Visitor<List<Integer>>() {

					@Override /**visitor pattern, double dispatch*/
					public List<Integer> visit(SingleMove move) {
						List<Integer> d=new ArrayList<>();
						d.add(move.destination);
						return d;
					}

					@Override
					public List<Integer> visit(DoubleMove move) {
						List<Integer> d=new ArrayList<>();
						d.add(move.destination1);
						d.add(move.destination2);
						return d;
					}
				});

				List<Ticket> MTicket=move.accept(new Visitor<List<Ticket>>() {
					@Override
					public List<Ticket> visit(SingleMove move) {
						List<Ticket> d=new ArrayList<>();
						d.add(move.ticket);
						return d;
					}

					@Override
					public List<Ticket> visit(DoubleMove move) {
						List<Ticket> d=new ArrayList<>();
						d.add(move.ticket1);
						d.add(move.ticket2);
						return d;
					}
				} );

				if(move.commencedBy()== MrX.MRX){
					if(MDestination.size()>1){

						/** double card situation*/
						if(setup.moves.get(log.size())==true&&setup.moves.get(log.size()+1)==true){ /**reveal reveal*/
							l.add(LogEntry.reveal(MTicket.get(0),MDestination.get(0)));
							l.add(LogEntry.reveal(MTicket.get(1),MDestination.get(1)));
						}
						else if (setup.moves.get(log.size())==true) { /**reveal hidden*/
							l.add(LogEntry.reveal(MTicket.get(0), MDestination.get(0)));
							l.add(LogEntry.hidden(MTicket.get(1)));
						}
						else if (setup.moves.get(log.size()+1)==true) { /** hidden reveal*/
							l.add(LogEntry.hidden(MTicket.get(0)));
							l.add(LogEntry.reveal(MTicket.get(1),MDestination.get(1)));
						}
						else{ /** hidden hidden*/
							l.add(LogEntry.hidden(MTicket.get(0)));
							l.add(LogEntry.hidden(MTicket.get(1)));
						}

						newDestination=MDestination.get(1);  /** get update*/
					}
					else{ /** single card situation*/
						if (setup.moves.get(log.size())==true){
							l.add(LogEntry.reveal(MTicket.get(0),MDestination.get(0)));
						}
						else{
							l.add(LogEntry.hidden(MTicket.get(0)));
						}
						newDestination= MDestination.get(0);
					}

					mrX=mrX.use(move.tickets());
					newlog=ImmutableList.copyOf(l);

					for(Player p:detectives){
						newRemaining.add(p.piece());
					}
					return new MyGameState(setup,ImmutableSet.copyOf(newRemaining),ImmutableList.copyOf(newlog),mrX.at(newDestination),detectives);

				}


				newDestination=MDestination.get(0);
				List<Player> newDetectives=new ArrayList<>();
				for(Player p:detectives){
					if(p.piece()==move.commencedBy()){
						p=p.use(move.tickets());
						p=p.at(newDestination);
					}
					newDetectives.add(p);
				}
			mrX=mrX.give(move.tickets()); /** Mr.X get ticket*/
			for(Piece p:remaining){ /** Judge whether the turn is over */
				if(p==move.commencedBy()){
					continue;
				}
				else{
					newRemaining.add(p);
				}
		}
			if (newRemaining.size() >0){
				return new MyGameState(setup,ImmutableSet.copyOf(newRemaining),log,mrX,newDetectives);
				}
			else{
				return new MyGameState(setup,ImmutableSet.of(MrX.MRX),log,mrX,newDetectives);
						}
		}
}


	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);


	}

}
