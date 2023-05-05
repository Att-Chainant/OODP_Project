import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.CRC32;
import MovieExceptions.*;

public class Program {
	private Terminal Console = new Terminal();
	private final String BRAND;
	private final String MOVIE_FREE_SOURCE = "MovieDataCenter.bak"; // Global any brand can used.
	private final String MOVIE_SOURCE; // Local data movie for each of brand has!
	private ArrayList<Movie> Movies = new ArrayList<Movie>(); // Collection (Caches)
	private ArrayList<Item> Shops = new ArrayList<Item>();
	private ArrayList<Movie> ScheduleMovies = new ArrayList<Movie>();
	private Database FileDatabase; // Database
	private HashMap<String, Integer> QueryNames = new HashMap<String, Integer>();
	private HashMap<String, Integer> QueryGenres = new HashMap<String, Integer>();
	private ArrayList<Movie> MoviesOnAir = new ArrayList<Movie>(); // Depend how many movieRooms have.
	private int MovieRooms = 100; // Room per movie
	private HashMap<String, Receipt> Receipts = new HashMap<String, Receipt>();
	private HashMap<String, String[][]> Seats = new HashMap<String, String[][]>();
	private boolean[] SchedulePerHour = new boolean[13];// 10:00, 11:00, ..., 22:00
	private static String SafeGet(String[] array, int index) { // Default-Value: ""
		if (index >= 0 && index < array.length) {
	        return array[index];
	    }
		return "";
	}
	private static int Clamp(int x, int min, int max) {
		return Math.max(min, Math.min(x, max)); // Reference from Lua, limit x by a range min and max.
	}
	private static String Comma(int number) {
	    DecimalFormat formatter = new DecimalFormat("#,###");
	    return formatter.format(number);
	}
	//...This array is designed to avoid replication of movie schedule time by movie lengthInMinutes.
	private void SetSeats(Movie v, int Pick, int ChunkHour, ArrayList<Integer> AvailableSchedule) {
		Console.Clear();
		int MovieOnAirIdx = 0;
		for (int l = 0; l < MoviesOnAir.size(); l++) {
			if (MoviesOnAir.get(l).Name() == v.Name()) {
				MovieOnAirIdx = l;
				break;
			}
		}
		int PlaceSchedule = AvailableSchedule.get(Pick - 1) - 10;
		String RoundSeat = (MovieOnAirIdx + ":" + PlaceSchedule); // UniqueKey for each hall seats with different of time schedule.
		String[][] HallSeats = Seats.getOrDefault(RoundSeat, new String[6][10]);
		System.out.println("############################################################");
		System.out.println("======================S==C==R==E==E==N======================");
		System.out.println("############################################################");
		System.out.println();
		for (int i = 0; i < HallSeats.length; ++i) {
            for(int j = 0; j < HallSeats[i].length; ++j) {
            	if (HallSeats[i][j] == null) {
            		System.out.print(String.format(" [%d%d] ", i, j));
            	} else {
            		System.out.print(" [--] ");
            	}
            }
            System.out.println();
        }
		System.out.println();
		String SeatLocations = Console.PromptString(String.format("| Hall#%d, select your seat", MovieOnAirIdx + 1), "00-59");
		if(SeatLocations.length() == 2) {
			try {
				int x = Integer.parseInt(("" + SeatLocations.charAt(0)));
				int y = Integer.parseInt(("" + SeatLocations.charAt(1)));
				if(HallSeats[x][y] == null) {
					// PASS (x and y is a number)
					for (int Place = PlaceSchedule; Place < PlaceSchedule + ChunkHour; Place++) {
						SchedulePerHour[Place] = true;
					}
					v.SetSchedule(PlaceSchedule, PlaceSchedule + ChunkHour);
					ScheduleMovies.add(v);
					v.UUID = UUID.randomUUID().toString();
					v.SeatIdx = new int[] {MovieOnAirIdx, x, y};
					HallSeats[x][y] = v.UUID;
					Seats.put(RoundSeat, HallSeats); // update
				} else {
					System.out.println("Someone already claim this seat, please find another one.");
					Console.Pause();
					
				}
			} catch (IndexOutOfBoundsException e) {
				System.out.println("Error: not found seat. (skipped process)");
				Console.Pause();
			} catch (NumberFormatException e) {
				System.out.println("Error: input must be a number format. (skipped process)");
				Console.Pause();
			}
		} else {
			System.out.println("Error: require 2 digits. (skipped process)");
			Console.Pause();
		}
	}
	private void MovieBooking(Movie v) {
		ArrayList<Integer> AvailableSchedule = new ArrayList<Integer>();
		int ChunkHour = (int) Math.ceil((double) v.LengthInMinutes() / 60); // v.LengthInMinutes must more than 30 and less than 660
		int RoundOverByDay = (SchedulePerHour.length - ChunkHour);
		for (int Period = 0; Period <= RoundOverByDay / ChunkHour; Period++) {
			int StartInScheduleUnit = (Period * ChunkHour);
			int LocalTime = StartInScheduleUnit + 10;
			boolean IsAvailable = true;
			for (int EachOf = StartInScheduleUnit; EachOf < StartInScheduleUnit + ChunkHour; EachOf++) {
				if (SchedulePerHour[EachOf]) {
					IsAvailable = false; // Found some rented schedule in period of time.
					break;
				}
			}
			if (IsAvailable) {
				AvailableSchedule.add(LocalTime);
			}
		}
		Console.Clear();
		System.out.println(String.format("[%s]: %d Minutes", v.Name(), v.LengthInMinutes()));
		System.out.println(String.format("- Available Schedule", v.Name()));
		for (int Index = 0; Index < AvailableSchedule.size(); Index++) {
			int AvailableTime = AvailableSchedule.get(Index);
			System.out.println(String.format("  (%d) %d:00 - %d:00", (Index + 1), AvailableTime, (AvailableTime + ChunkHour)) );
		}
		int BackMenu = AvailableSchedule.size() + 1;
		System.out.println(String.format("  (%d) Back", BackMenu));
		int Pick = Console.PromptInteger("| Now, pick a schedule time", 1, BackMenu);
		if (Pick != BackMenu) {
			SetSeats(v, Pick, ChunkHour, AvailableSchedule);
		}
	}
	private void DisconnectReceipt(String movieName) { // this method will clear all receipt that has same movieName from input.
		for (Map.Entry<String, Receipt> Set : Receipts.entrySet()) {
			Receipt v2 = Set.getValue();
			if (movieName == v2.MovieName()) {
				int[] SeatIdx = v2.Seat();
				int[] ScheduleRange = v2.Schedule();
				String RoundSeat = (SeatIdx[0] + ":" + ScheduleRange[0]);
				String[][] HallSeats = Seats.getOrDefault(RoundSeat, new String[6][10]);
				HallSeats[SeatIdx[1]][SeatIdx[2]] = null;
				Seats.put(RoundSeat, HallSeats);
				Receipts.remove(Set.getKey());
			}
		}
	}
	private void DisconnectScheduleMovie(String movieName) {
		Iterator<Movie> iterator = ScheduleMovies.iterator();
		while (iterator.hasNext()) {
			Movie onQueue = iterator.next();
		    if (onQueue.Name() == movieName) {
				int[] ScheduleRange = onQueue.GetSchedule();
				if (ScheduleRange != null) {
					for (int Place = ScheduleRange[0]; Place < ScheduleRange[1]; Place++) {
						SchedulePerHour[Place] = false; // free the memory (w/ period of time)
					}
					onQueue.SetSchedule(); // clear!
				}
				iterator.remove();
				int[] SeatIdx = onQueue.SeatIdx;
				String RoundSeat = (SeatIdx[0] + ":" + ScheduleRange[0]); // UniqueKey for each hall seats with different of time schedule.
				String[][] HallSeats = Seats.getOrDefault(RoundSeat, new String[6][10]);
				HallSeats[SeatIdx[1]][SeatIdx[2]] = null; // set MovieTicket to null
				Seats.put(RoundSeat, HallSeats); // update
				break;
			}
		}
	}
	private void PrintScheduleMovies() {
		if (ScheduleMovies.size() > 0) {
			System.out.println("::: Your currently schedule movies. :::");
			Comparator<Movie> ScheduleOrderComparator = new Comparator<Movie>() {
			    @Override
			    public int compare(Movie v1, Movie v2) {
			        return v1.GetSchedule()[0] - v2.GetSchedule()[0];
			    }
			};
			ScheduleMovies.sort(ScheduleOrderComparator);
			for (int Index = 0; Index < ScheduleMovies.size(); Index++) {
				Movie v = ScheduleMovies.get(Index);
				int[] schedu = v.GetSchedule();
				String MovieDurationInClockSeconds = String.format("%02d:%02d:00", (int) v.LengthInMinutes() / 60, v.LengthInMinutes() % 60);
				System.out.println(String.format(" (%d) [%s] // %d:00 - %d:00 (%s)", Index + 1, v.Name(), schedu[0] + 10, schedu[1] + 10, MovieDurationInClockSeconds));
			}
		}
	}
	private ArrayList<Movie> AvailableMovies(ArrayList<Movie> Original) {
		ArrayList<Movie> Result = new ArrayList<Movie>();
		int LeftMovieLengthApprox = SchedulePerHour.length; // Unit is hour
		for (boolean x: SchedulePerHour) {
			LeftMovieLengthApprox += x ? -1 : 0;
		}
		for (Movie v: Original) {
			int[] Period = v.GetSchedule();
			if (Period == null) {
				int ChunkHour = (int) Math.ceil((double) v.LengthInMinutes() / 60);
				if (ChunkHour < LeftMovieLengthApprox - 1) {
					Result.add(v);
				}
			}
		}
		return Result;
	}
	private ArrayList<Movie> AvailableMovies() {
		return AvailableMovies(MoviesOnAir);
	}
	private void BookingsPage() {
		boolean Running = true;
		do {
			Console.Clear();
			PrintScheduleMovies();
			System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Bookings"));
			System.out.println(" (1) ::ALLs::");
			System.out.println(" (2) ::Genres::");
			System.out.println(" (3) Remove schedule");
			System.out.println(" (4) </Confirm>");
			System.out.println(" (5) Back");
			int Choice = Console.PromptInteger("| Please, select an action", 1, 5);
			Console.Clear();
			PrintScheduleMovies();
			ArrayList<Movie> ScopeMovies = AvailableMovies();
			switch (Choice) {
				case 1:
					System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Bookings / ALLs"));
					for (int Index = 0; Index < ScopeMovies.size(); Index++) {
						Movie v = ScopeMovies.get(Index);
						System.out.println(String.format(" (%d) %s, %d Minutes", Index + 1, v.Name(), v.LengthInMinutes()));
					}
					int BackMenu1 = ScopeMovies.size() + 1;
					System.out.println(String.format(" (%d) Back", BackMenu1));
					int Pick1 = Console.PromptInteger("| Please, select a movie", 1, BackMenu1);
					if (BackMenu1 != Pick1) {
						MovieBooking(ScopeMovies.get(Pick1 - 1));
					}
					break;
				case 2:		
					System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Bookings / Genres"));
					Set<String> x = QueryGenres.keySet();
					ArrayList<String> li = new ArrayList<String>(x);
					ArrayList<String> res = new ArrayList<String>();
					for (int i = 0; i < li.size(); i++) {
						ArrayList<Movie> MoviesPerGenre = GetMovies(li.get(i)); // OnAir
						if (MoviesPerGenre.size() > 0) {
							res.add(li.get(i));
						}
					}
					String[] arr = res.toArray(new String[0]);
					for (int i = 0; i < arr.length; i++) {
						ArrayList<Movie> MoviesPerGenre = GetMovies(arr[i]); // OnAir
						System.out.println(String.format(" (%s) %s: %d movies", i + 1, arr[i], MoviesPerGenre.size()));
					}
					int BackMenu2 = arr.length + 1;
					System.out.println(String.format(" (%d) Back", BackMenu2));
					int Pick2 = Console.PromptInteger("| Please, select a genre", 1, BackMenu2);
					if (BackMenu2 != Pick2) {
						Console.Clear();
						String Genre = SafeGet(arr, Pick2 - 1);
						if (QueryGenres.getOrDefault(Genre, 0) > 0) {
							PrintScheduleMovies();
							System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Bookings / Genres / " + Genre));
							ArrayList<Movie> QueryMovies = GetMovies(Genre);
							QueryMovies = AvailableMovies(QueryMovies); // Filter (LengthInMinutes, Schedule...)
							for (int Index = 0; Index < QueryMovies.size(); Index++) {
								Movie v = QueryMovies.get(Index);
								System.out.println(String.format(" (%d) %s, %d Minutes", Index + 1, v.Name(), v.LengthInMinutes()));
							}
							int BackMenu2_1 = QueryMovies.size() + 1;
							System.out.println(String.format(" (%d) Back", BackMenu2_1));
							int Pick2_1 = Console.PromptInteger("| Please, select a movie", 1, BackMenu2_1);
							if (BackMenu2_1 != Pick2_1) {
								MovieBooking(QueryMovies.get(Pick2_1 - 1));
							}
						} else {
							System.out.println("Not found the topic, from the user input.");
							Console.Pause();
						}
					}
					break;
				case 3:
					if (ScheduleMovies.size() > 0) {
						int BackMenu3 = ScheduleMovies.size() + 1;
						System.out.println(String.format(" (%d) Back", BackMenu3));
						int RemoveAt = Console.PromptInteger("| Select a movie to remove out", 1, BackMenu3);
						if (BackMenu3 != RemoveAt) {
							Movie v = ScheduleMovies.remove(RemoveAt - 1);
							int[] ScheduleRange = v.GetSchedule();
							for (int Place = ScheduleRange[0]; Place < ScheduleRange[1]; Place++) {
								SchedulePerHour[Place] = false; // free the memory (w/ period of time)
							}
							int[] SeatIdx = v.SeatIdx;
							String RoundSeat = (SeatIdx[0] + ":" + ScheduleRange[0]); // UniqueKey for each hall seats with different of time schedule.
							String[][] HallSeats = Seats.getOrDefault(RoundSeat, new String[6][10]);
							HallSeats[SeatIdx[1]][SeatIdx[2]] = null; // set MovieTicket to null
							Seats.put(RoundSeat, HallSeats); // update
							v.SetSchedule();
							v.UUID = null;
							v.SeatIdx = null;
							DisconnectReceipt(v.Name());
							Console.Pause();
						}
					} else {
						System.out.println("::: No any movies that book in schedule. :::");
						Console.Pause();
					}
					break;
				case 4:
					ConfirmPage();
					break;
				default:
					Running = false;
					break;
			}
		} while (Running);
	}
	private void ConfirmPage() {
		for (int i = 0; i < ScheduleMovies.size(); i++) {
			Movie v = ScheduleMovies.get(i);  // keep in mind, this only clear all queue movies that you select. but not from seat ticket
			int[] ScheduleRange = v.GetSchedule();
			System.out.println(String.format("[%s]:", v.UUID));
			System.out.println(String.format("   - Movie: %s (%d Minutes), by %s", v.Name(), v.LengthInMinutes(), v.Director()));
			System.out.println(String.format("   - Period: %d:00 - %d:00", ScheduleRange[0] + 10, ScheduleRange[1] + 10));
			System.out.println(String.format("   - Seat: Room #%d, Number %s", v.SeatIdx[0] + 1, v.SeatIdx[1] + "" + v.SeatIdx[2]));
			for (int Place = ScheduleRange[0]; Place < ScheduleRange[1]; Place++) {
				SchedulePerHour[Place] = false; // free the memory (w/ period of time)
			}
			Receipts.put(v.UUID, new Receipt(v.Name(), v.SeatIdx, v.GetSchedule()));
			v.SetSchedule();
			v.UUID = null;
			v.SeatIdx = null;
		}
		if (ScheduleMovies.size() > 0) {
			ScheduleMovies.clear();
			Console.Pause();
		}
	}
	private void ConfigsPage() {
		boolean Running = true;
		do {
			Console.Clear();
			System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Configs"));
			System.out.println(" (1) Add movie to database");
			System.out.println(" (2) Remove movie to database");
			System.out.println(" (3) Insert movie from database, into on-air channel.");
			System.out.println(" (4) Take movie out from on-air channel.");
			System.out.println(" (5) Back");
			int Choice = Console.PromptInteger("| Please, select an action", 1, 5);
			Console.Clear();
			switch (Choice) {
				case 1:
					System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Configs / ADD:MOVIE"));
					try {
						String MOVIE_NAME = String.join("", Console.PromptString("| MOVIE_NAME", "String").split(","));
						String[] MOVIE_GENRES = Console.PromptString("| MOVIE_GENRES", "String, ...").split(", ");
						int MOVIE_DURATION = Integer.parseInt( Console.PromptString("| MOVIE_DURAION", "int: CLAMP<30, 660>") ); // NumberFormatException
						String[] MOVIE_DIRECTORS = Console.PromptString("| MOVIE_DIRECTORS", "String, ...").split(", ");
						Add(MOVIE_NAME, MOVIE_GENRES, MOVIE_DURATION, MOVIE_DIRECTORS);
					} catch (NumberFormatException | InvalidMovieData | MovieDataAlreadyExist e1) {
						e1.printStackTrace();
						Console.Pause();
					}
					break;
				case 2:
					System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Configs / REMOVE:MOVIE"));
					for (int Index = 0; Index < Movies.size(); Index++) {
						Movie v = Movies.get(Index);
						System.out.println(String.format(" (%d) %s", Index + 1, v.Name()));
					}
					int BackMenu = Movies.size() + 1;
					System.out.println(String.format(" (%d) Back", BackMenu));
					int RemoveAt = Console.PromptInteger("| MOVIE_INDEX", 1, BackMenu);
					if (BackMenu != RemoveAt) {
						try {
							Movie v = Movies.get(RemoveAt - 1);
							Remove(v.Name()); // clear from collection (main database)
							for (int i = MoviesOnAir.size() - 1; i >= 0; i--) {
								if (v.Name() == MoviesOnAir.get(i).Name()) {
									MoviesOnAir.remove(i);
									break;
								}
							} // clear that movie (state in remove) out from MoviesOnAir
							DisconnectScheduleMovie(v.Name());
							DisconnectReceipt(v.Name());
						} catch (IndexOutOfBoundsException | InvalidMovieData | MovieDataNoLongerExist e) {
							e.printStackTrace();
							Console.Pause();
						}
					}
					break;
				case 3:
					if (MoviesOnAir.size() >= MovieRooms) {
						System.out.println(String.format("Currently on-air channel is full (limit: %d), try to take movie out from on-air channel first.", MovieRooms));
						Console.Pause();
						break;
					}
					System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Configs / ON-AIR:ADD"));
					HashMap<String, Boolean> OnAir = new HashMap<String, Boolean>();
					ArrayList<Movie> OffAir = new ArrayList<Movie>();
					for (int i = 0; i < MoviesOnAir.size(); i++) {
						Movie v = MoviesOnAir.get(i);
						OnAir.put(v.Name(), true);
					}
					int Index = 0;
					for (int i = 0; i < Movies.size(); i++) {
						Movie v = Movies.get(i);
						if (!OnAir.getOrDefault(v.Name(), false)) {
							OffAir.add(v);
							System.out.println(String.format(" (%d) %s", Index + 1, v.Name()));
							Index++;
						}
					}
					int BackMenuOAD = OffAir.size() + 1; System.out.println(String.format(" (%d) Back", BackMenuOAD));
					int AddIdxOAD = Console.PromptInteger("| Select movie to add into on-air channel", 1, BackMenuOAD);
					if (BackMenuOAD != AddIdxOAD) {
						Movie v;
						try {
							v = OffAir.get(AddIdxOAD - 1);
							v = v.clone();
							MoviesOnAir.add(v);
						} catch (IndexOutOfBoundsException | CloneNotSupportedException e) {
							e.printStackTrace();
							Console.Pause();
						}
					}
					break;
				case 4:
					System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Configs / ON-AIR:REMOVE"));
					for (int i = 0; i < MoviesOnAir.size(); i++) {
						Movie v = MoviesOnAir.get(i);
						System.out.println(String.format(" (%d) %s", i + 1, v.Name()));
					}
					int BackMenuOAR = MoviesOnAir.size() + 1; System.out.println(String.format(" (%d) Back", BackMenuOAR));
					int RemoveIdxOAR = Console.PromptInteger("| Select movie to take out from on-air channel", 1, BackMenuOAR);
					if (BackMenuOAR != RemoveIdxOAR) {
						Movie v = MoviesOnAir.get(RemoveIdxOAR - 1);
						MoviesOnAir.remove(RemoveIdxOAR - 1);
						DisconnectScheduleMovie(v.Name());
						DisconnectReceipt(v.Name());
						Console.Pause();
					}
					break;
				default:
					Running = false;
					break;
			}
		} while (Running);
	}
	private void StoresPage() {
		boolean Running = true;
		do {
			Console.Clear();
			System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, "Stores"));
			for (int Index = 0; Index < Shops.size(); Index++) {
				Item v = Shops.get(Index);
				System.out.println(String.format(" (%d) %s, %d baht %s", Index + 1, v.Name(), v.Price(), v.Amount > 0 ? ("(x" + v.Amount + ")") : ""));
			}
			int ConfirmMenu = Shops.size() + 1;
			System.out.println(String.format(" (%d) </Confirm>", ConfirmMenu));
			int BackMenu = Shops.size() + 2;
			System.out.println(String.format(" (%d) Back", BackMenu));
			int Choice = Console.PromptInteger("| Please, select an action", 1, BackMenu);
			if (Choice == ConfirmMenu) {
				int TotalPrice = 0;
				for (Item v: Shops) {
					if (v.Amount > 0) {
						TotalPrice += (v.Amount * v.Price());
					}
				}
				Console.Clear();
				if (TotalPrice > 0) {
					System.out.println(String.format("[%s]:", UUID.randomUUID().toString()));
					for (Item v: Shops) {
						if (v.Amount > 0) {
							int SumPrice = (v.Amount * v.Price());
							System.out.println(String.format("   - Product: %s (x%d)", v.Name(), v.Amount));
							System.out.println(String.format("      - SumPrice: %s baht", Comma(SumPrice)));
							v.Amount = 0;
						}
					}
					System.out.println(String.format("   TOTAL_PRICE: %s baht", Comma(TotalPrice)));
				} else {
					System.out.println("::: No any product in cart. :::");
				}
				Console.Pause();
			} else if (Choice == BackMenu) {
				Running = false;
			} else {
				Item v = Shops.get(Choice - 1);
				try {
					int AMOUNT = Integer.parseInt(Console.PromptString(String.format("| Set your demand of an item", v.Name()), "int: CLAMP<0, 99>"));
					v.Amount = Clamp(AMOUNT, 0, 99); // max-stack is 99
				} catch (NumberFormatException e) {
					System.out.println("Error: your input is not a integer type.");
					Console.Pause();
				}
				
			}
		} while (Running);
	}
	// + Public
	public void Add(String MovieName, String[] MovieGenre, int MovieDuration, String[] MovieDirector) throws InvalidMovieData, MovieDataAlreadyExist {
		MovieDuration = Clamp(MovieDuration, 30, 660);
		if (MovieName == "" || MovieName == null) { throw new InvalidMovieData("MovieName, must not be empty string or null."); }
		if (MovieGenre.length > 0) {
			for (int i = 0; i < MovieGenre.length; i++) {
				if (SafeGet(MovieGenre, i) == "") { throw new InvalidMovieData("MovieGenre[], elements must not be empty string or null."); }
			}
		} else { throw new InvalidMovieData("MovieGenre[], must has one element or more."); }
		if (MovieDuration <= 0) { throw new InvalidMovieData("MovieDuration, must be more than zero."); }
		if (MovieDirector.length > 0) {
			for (int i = 0; i < MovieDirector.length; i++) { 
				if (SafeGet(MovieDirector, i) == "") { throw new InvalidMovieData("MovieDirector[], elements must not be empty string or null."); }
			}
		} else { throw new InvalidMovieData("MovieDirector[], must has one element or more."); }
		// Function
		int MovieId = QueryNames.getOrDefault(MovieName, -1);
		if (MovieId == -1) {
			MovieId = Movies.size(); // add to last
	        Movies.add(MovieId, new Movie(MovieName, MovieGenre, MovieDuration, MovieDirector));
	        QueryNames.put(MovieName, MovieId);
	        for (String MovieType: MovieGenre) {
	        	QueryGenres.put(MovieType, QueryGenres.getOrDefault(MovieType, 0) + 1);
	        }
	        FileDatabase.UPDATE();
		} else {
			throw new MovieDataAlreadyExist(String.format("%s -> %d", MovieName, MovieId));
		}
	}
	public void Remove(String MovieName) throws InvalidMovieData, MovieDataNoLongerExist {
		if (MovieName == "" || MovieName == null) { throw new InvalidMovieData("MovieName, must not be empty string or null."); }
		int MovieId = QueryNames.getOrDefault(MovieName, -1);
		if (MovieId == -1) {
			throw new MovieDataNoLongerExist(MovieName);
		} else {
			QueryNames.remove(MovieName);
			Movie v = Movies.remove(MovieId);
			for (Map.Entry<String, Boolean> Set : v.Genres().entrySet()) {
				String Genre = Set.getKey();
				int Amount = QueryGenres.getOrDefault(Genre, 1);
				Amount = Amount - 1;
				if (Amount <= 0) {
					QueryGenres.remove(Genre);
				} else {
					QueryGenres.put(Genre, Amount);
				}
			}
			FileDatabase.UPDATE();
		}
	}
	public ArrayList<Movie> GetMovies(String QueryGenre) {
		ArrayList<Movie> Result = new ArrayList<Movie>();
		for (Movie v: MoviesOnAir) {
			if (v.Genres().getOrDefault(QueryGenre, false)) {
				Result.add(v);
			}
		}
		return Result;
	}
	// Constructor
	public Program(String Brand) {
		this.BRAND = Brand;
		CRC32 crc32 = new CRC32();
		crc32.update(Brand.getBytes());
		String Token = Long.toHexString(crc32.getValue());
		this.MOVIE_SOURCE = Token; // Set Source (Unique - Key)
		//:: Synchronize Data ::
		Database Stores = new Database(new File(MOVIE_SOURCE + ".stor"), Shops);
		String StoresContent = Stores.READ(false);
		if (StoresContent != null) {
			try (BufferedReader Reader = new BufferedReader(new StringReader(StoresContent))) {
				String Line = null;
				while ((Line = Reader.readLine()) != null) {
			        String[] Info = Line.split(", ");
			        String ItemName = SafeGet(Info, 0);
			        int ItemPrice = Integer.parseInt(SafeGet(Info, 1));
			        int ItemId = Shops.size();
			        Shops.add(ItemId, new Item(ItemName, ItemPrice));
				}
			} catch (IOException e) {}
		} else {
			System.out.println("::Program closed, due to missing store/shop file.");
			Console.Close();
		}
		File File = new File(MOVIE_SOURCE + ".dat");
		FileDatabase = new Database(File, Movies); // Initialize self: Database by own collection.
		String FileContent = FileDatabase.READ(true);
		if (FileContent != null) {
			// **Better than using FileContent.split("\n") + for loop check after which is around O(n*2)
			try (BufferedReader Reader = new BufferedReader(new StringReader(FileContent))) {
				String Line = null;
				while ((Line = Reader.readLine()) != null) {
			        String[] Info = Line.split(", ");
			        String MovieName = SafeGet(Info, 0);
			        String[] MovieGenre = SafeGet(Info, 1).split("/");
			        int MovieDuration = Integer.parseInt(SafeGet(Info, 2));
			        String[] MovieDirector = SafeGet(Info, 3).split(" and ");
			        // Database -> Collection (Caches)
			        int MovieId = Movies.size();
			        Movies.add(MovieId, new Movie(MovieName, MovieGenre, MovieDuration, MovieDirector));			        
			        QueryNames.put(MovieName, MovieId);
			        for (String MovieType: MovieGenre) {
			        	QueryGenres.put(MovieType, QueryGenres.getOrDefault(MovieType, 0) + 1);
			        	// In Lua, QueryGenres[MovieType] = QueryGenres[MovieType] and QueryGenres[MovieType] + 1 or 1
			        }
				}
			} catch (IOException e) {} // Not happen for sure.
		} else {
			File BackupFile = new File(MOVIE_FREE_SOURCE);
			String BackupFileContent = FileDatabase.READFILE(BackupFile, false);
			if (BackupFileContent != null) {
				System.out.println("Due the local movie database file is missing, loaded movie from data center (.bak) -> successfully.");
				FileDatabase.WRITE(BackupFileContent);
				System.out.println("::Program need to be restart...");
				Console.Pause();
			} else {
				if (Console.PromptBoolean("System not found any of movie database file, do you want to create new one?")) {
					FileDatabase.WRITE("Interstellar, Sci-Fi/Adventure, 169, Christopher Nolan" + "\n" +
							"The Lord of the Rings: The Two Towers, Adventure/Fantasy, 179, Peter Jackson" + "\n" + 
							"The Matrix, Action/Sci-Fi, 136, The Wachowskis" + "\n" +
							"Psycho, Horror/Thriller, 109, Alfred Hitchcock" + "\n" +
							"A Clockwork Orange, Sci-Fi/Drama, 136, Stanley Kubrick" + "\n"); // Sample MovieData.
					System.out.println("::Program need to be restart...");
				} else {
					System.out.println("::Program closed, due to missing movie database file.");
					Console.Pause();
				}
			}
			Console.Close();
		}
		// Set Movie On Air (Random)
		Random Randomzier = new Random();
		ArrayList<Movie> Clone = new ArrayList<Movie>();
		Clone.addAll(Movies); // Clone elements in Movies.
		if (Movies.size() > MovieRooms) {
			for (int i = 0; i < MovieRooms; i++) {
				int Pick = Randomzier.nextInt(Clone.size() - 1);
				Movie __Movie__ = Clone.remove(Pick);
				MoviesOnAir.add(__Movie__);
			}
		} else {
			MoviesOnAir = Clone; // no need to use MoviesOnAir.addAll(Clone); which is gonna take a lot more runtime O(n)
		}
	}
	public void Start() {
		boolean Running = true;
		do {
			Console.Clear();
			System.out.println(String.format("::: %s Theater - Program ::: (%s)", this.BRAND, this.MOVIE_SOURCE));
			System.out.println(" (1) Bookings");
			System.out.println(" (2) Stores");
			System.out.println(" (3) </Configs>");
			System.out.println(" (4) </Exit>");
			int Choice = Console.PromptInteger("| Please, select an action", 1, 5); 
			Console.Clear();
			switch (Choice) {
				case 1:
					BookingsPage();
					break;
				case 2:
					StoresPage();
					break;
				case 3:
					String keyPass = Console.PromptString("PASSWORD", "Hex");
					if (keyPass.equals(this.MOVIE_SOURCE)) {
						ConfigsPage();
					}
					break;
				default: // case 5:
					Running = false;
					break;
			}
		} while (Running);
		Console.Close();
	}
}