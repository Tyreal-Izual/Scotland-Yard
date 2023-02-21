package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;



/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements  Model{

		private GameSetup setup;
		private Set<Observer> observers;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private Board.GameState state;


		private MyModel(
				final GameSetup setup,
				final Player mrX,
				final ImmutableList<Player> detectives){




			this.setup = setup;
			this.mrX = mrX;
			this.detectives = detectives;
			this.observers = new HashSet<>();
			this.state = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		/**
		 *
		 * @returns the current GameState
		 */
		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		/**
		 *
		 * @param observer the observer to register
		 */
		@Override
		public void registerObserver(@Nonnull Observer observer) { /** Adds a new observer if he is not registered yet */
			if(observers.contains(observer))
				throw new IllegalArgumentException("This observer is already registered!");
			if(observer == null)
				throw new NullPointerException();
			observers.add(observer);
		}

		/**
		 *
		 * @param observer the observer to register
		 */
		@Override
		public void unregisterObserver(@Nonnull Observer observer) { /** Removes a given observer if he is part of the list */
			if(observer == null)
				throw new NullPointerException();
			if(!observers.contains(observer))
				throw new IllegalArgumentException("Not a registered observer");
			observers.remove(observer);

		}

		/**
		 *
		 * @returns all the observers
		 */
		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		/**
		 *
		 * @param move delegates the move to the underlying
		 */
		@Override
		public void chooseMove(@Nonnull Move move) { /** Notifies all the observers */
			Observer.Event event;

			this.state = this.state.advance(move); /** get advance move*/
			if(state.getWinner().isEmpty())
				event = Observer.Event.MOVE_MADE;
			else event = Observer.Event.GAME_OVER;
			for(Observer observer : observers)
				observer.onModelChanged(this.state,event);



		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {

		MyModel myModel = new MyModel(setup, mrX, detectives);
		return myModel;
	}
}
