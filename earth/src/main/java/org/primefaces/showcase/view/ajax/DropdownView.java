package org.primefaces.showcase.view.ajax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.maltparser.core.helper.HashSet;
import org.ovgu.de.fiction.model.TopKResults;
import org.ovgu.de.fiction.search.FictionRetrievalSearch;
import org.ovgu.de.fiction.search.InterpretSearchResults;
import org.ovgu.de.fiction.utils.FRConstants;
import org.ovgu.de.fiction.utils.FRGeneralUtils;
import org.ovgu.de.fiction.utils.FRWebUtils;
import org.primefaces.model.StreamedContent;
import org.primefaces.showcase.convert.CarService;
import org.primefaces.showcase.domain.BookUI;

@ManagedBean
@ViewScoped
public class DropdownView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 462006850003220169L;
	private Map<String, List<String>> data = new HashMap<String, List<String>>();
	private String genre;
	private String book;
	private String txt;
	private String selBook;
	private Map<String, String> genreMap;
	private List<String> books;
	private boolean shallShowTable;
	private List<BookUI> simBooks;
	private String showMsg;
	private StreamedContent file;

	private byte[] exportContent;

	@ManagedProperty("#{carService}")
	private CarService service;

	public boolean isReady() {
		return exportContent != null;
	}

	@PostConstruct
	public void init() {
		System.out.println("fa " + shallShowTable);

		shallShowTable = false;
		genreMap = new HashMap<String, String>();
		genreMap.put("All", "All");
		genreMap.put("Allegories", "Allegories");
		genreMap.put("Christmas Stories", "Christmas Stories");
		genreMap.put("Detective and Mystery", "Detective and Mystery");
		genreMap.put("Ghost and Horror", "Ghost and Horror");
		genreMap.put("Humorous , Wit and Satire", "Humorous , Wit and Satire");
		genreMap.put("Literary", "Literary");
		genreMap.put("Love and Romance", "Love and Romance");
		genreMap.put("Sea and Adventure", "Sea and Adventure");
		genreMap.put("Western Stories", "Western Stories");

		data.put("Literary", generateLiteraryMap());
		data.put("Detective and Mystery", generateDetectiveMap());
		data.put("Western Stories", generateWesternMap());
		data.put("Ghost and Horror", generateGhostMap());
		data.put("Humorous , Wit and Satire", generateHumourMap());
		data.put("Christmas Stories", generateChristmasMap());
		data.put("Love and Romance", generateLoveMap());
		data.put("Sea and Adventure", generateSeaMap());
		data.put("Allegories", generateAllegoryMap());
		data.put("All", generateAll());
	}

	public Map<String, List<String>> getData() {
		return data;
	}

	public void onGenreChange() {
		System.out.println(shallShowTable);
		if (genre != null && !genre.equals(""))
			books = data.get(genre);
		else
			books = data.get("All");

		this.book = null;
	}

	// display
	public void displayBook() throws IOException {

		if (genre == null || genre.trim().equals("")) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Please select a Genre"));
			return;
		}

		if (book == null || book.trim().equals("")) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Please select a Book"));
			return;
		}

		if (book != null && genre != null) {
			shallShowTable = true;
			System.out.println("can see 1" + shallShowTable);

			// start book
			String similarity = "L2";

			FRWebUtils utils = new FRWebUtils();
			Map<String, String> book_master = utils.getAllMasterBooks(); // key = bookId, Value = Book_Name
			String qryBookId = utils.getMasterBookId(book_master, book);
			System.out.println("---------" + qryBookId);
			String FEATURE_CSV_FILE = FRGeneralUtils.getPropertyVal("file.feature");

			Map<String, Map<String, String>> stats_Of_results = new HashMap<>();

			if (!qryBookId.equals("")) {
				TopKResults topKResults = FictionRetrievalSearch.findRelevantBooks(qryBookId, FEATURE_CSV_FILE,
						FRConstants.SIMI_PENALISE_BY_NOTHING, FRConstants.SIMI_ROLLUP_BY_ADDTN,
						FRConstants.SIMI_EXCLUDE_TTR_NUMCHARS, FRConstants.TOP_K, similarity);

				InterpretSearchResults interp = new InterpretSearchResults();

				try {
					stats_Of_results = interp.performStatiscalAnalysis(topKResults);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				int rank = 0;
				simBooks = new ArrayList<>();
				for (Map.Entry<Double, String> res : topKResults.getResults_topK().entrySet()) {

					String[] bookArr = utils.getMasterBookName(book_master, String.valueOf(res.getValue())).split("#");
					if (bookArr.length<2 )
						continue;
					
					String bookName = bookArr[0];
					bookName = bookName.contains("|") ? bookName.substring(bookName.indexOf("#") + 1).replace("|", ",")
							: bookName.substring(bookName.indexOf("#") + 1);
					String bookId = utils.getMasterBookId(book_master, bookName);

					if (bookId.equals(qryBookId))
						continue;

					rank++;
					if (rank == FRConstants.TOP_K + 1)
						break;
System.out.println(bookArr[1]);
					String authName = bookArr[1].contains("|") ? bookArr[1].replace("|", ",") : bookArr[1];

					BookUI book = new BookUI();
					book.setId(bookId);
					book.setName(bookName);
					book.setAuthor(authName);
					book.setRank(rank);
					book.setEpubPath("/earth/epub/" + bookId + ".epub");
					book.setScore(String.valueOf(res.getKey()));

					simBooks.add(book);

				}

				System.out.println("books added " + simBooks.size());

				if (stats_Of_results.size() > 0) {
					// Map<String, String> correlations = new HashMap<>();
					Map<String, String> reduced_features = new HashMap<>();

					// correlations = stats_Of_results.get("CORR");
					reduced_features = stats_Of_results.get("FEAT");
					// String[] correAtrib = new String[correlations.size()];
					StringBuffer reducedFe = new StringBuffer(
							"Some important factors responsible for the list obtained below : ");

					Set<String> ftrSet = new HashSet<>();
					if (reduced_features.size() > 0) {
						for (Map.Entry<String, String> reduced_fe : reduced_features.entrySet()) {
							if (reduced_fe.getKey().startsWith("Feature")) {
								ftrSet.addAll(FRWebUtils.getFeatureHighLevelName(reduced_fe.getValue()));
							}
						}
					}

					for (String s : ftrSet) {
						reducedFe.append(s).append(" ,");
					}
					reducedFe.deleteCharAt(reducedFe.length() - 1);
					
					FacesMessage msg = new FacesMessage("Analysis could not be done");
					;

					if (reducedFe != null) {
						msg = new FacesMessage(FacesMessage.SEVERITY_INFO, null, reducedFe.toString());
					}

					FacesContext.getCurrentInstance().addMessage(null, msg);
					// "Writing Style, Male Prepositions, Positive Sentiment are top three
					// characteristics of the results"
				}
			}

		}
	}

	private List<String> generateHumourMap() {
		List<String> hum = new ArrayList<>();
		hum.add("Plain Mary Smith: A Romance of Red Saunders");
		hum.add("The Sin of Monsieur Pettipon");
		hum.add("Simon the Jester");
		hum.add("Upsidonia");
		hum.add("Danny's Own Story");
		hum.add("Once Aboard the Lugger");
		Collections.sort(hum);
		return hum;

	}

	private List<String> generateAllegoryMap() {
		List<String> allg = new ArrayList<>();
		allg.add("Allegories of Life");
		allg.add("The Unknown Sea");

		return allg;
	}

	private List<String> generateSeaMap() {
		List<String> sea = new ArrayList<>();
		sea.add("Sandy");
		sea.add("The Half-Hearted");
		sea.add("Overdue: The Story of a Missing Ship");
		sea.add("The Ghost Ship: A Mystery of the Sea");
		sea.add("Rattlin the Reefer");
		sea.add("Blacksheep! Blacksheep!");
		sea.add("A Chinese Command: A Story of Adventure in Eastern Seas");
		sea.add("Dick Leslie's Luck: A Story of Shipwreck and Adventure");
		sea.add("An Old Sailor's Yarns");
		sea.add("Daisy Brooks- A Perilous Love");
		sea.add("Kidnapped at the Altar- The Romance of that Saucy Jessie Bain");
		sea.add("The Recipe for Diamonds");
		sea.add("Command");
		sea.add("A Marriage at Sea");
		sea.add("Gerald Fitzgerald- the Chevalier: A Novel");
		sea.add("Lost Lenore: The Adventures of a Rolling Stone");
		sea.add("The Secret of the Sands- The 'Water Lily' and her Crew");
		sea.add("The Red Derelict");
		sea.add("In Strange Company: A Story of Chili and the Southern Seas");
		sea.add("In the Hands of the Malays- and Other Stories");
		sea.add("My Danish Sweetheart: A Novel. Volume 1 of 3");
		sea.add("My Danish Sweetheart: A Novel. Volume 2 of 3");
		sea.add("My Danish Sweetheart: A Novel. Volume 3 of 3");
		sea.add("The Adventurers");
		sea.add("The Bee Hunters: A Tale of Adventure");
		sea.add("The Smuggler Chief: A Novel");
		sea.add("A Soldier's Daughter- and Other Stories");
		sea.add("The Fashionable Adventures of Joshua Craig: A Novel");
		sea.add("Youth- a Narrative");
		sea.add("The Gentleman: A Romance of the Sea");
		sea.add("The Brown Mask");
		sea.add("The Bronze Bell");
		sea.add("Mardi: and A Voyage Thither I");
		sea.add("Mardi: and A Voyage Thither II");
		sea.add("Moby Dick");
		sea.add("Omoo Adventures in the South Seas");
		Collections.sort(sea);
		return sea;

	}

	private List<String> generateLoveMap() {
		List<String> love = new ArrayList<>();
		love.add("Charles Rex");
		love.add("The End of the World: A Love Story");
		love.add("The Laurel Bush: An Old-Fashioned Love Story");
		love.add("The Moon out of Reach");
		love.add("Arms and the Woman");
		love.add("The Romance of an Old Fool");
		love.add("In Apple-Blossom Time: A Fairy-Tale to Date");
		love.add("Doctor Luttrell's First Patient");
		love.add("I- Thou- and the Other One: A Love Story");
		love.add("A Song of a Single Note: A Love Story");
		love.add("The Letter of Credit");
		love.add("Mavis of Green Hill");
		love.add("God's Good Man: A Simple Love Story");
		love.add("The Lilac Sunbonnet: A Love Story");
		love.add("Innocent : her fancy and his fact");
		love.add("Their Yesterdays");
		love.add("A Day of Fate");
		love.add("Three Weeks");
		Collections.sort(love);
		return love;
	}

	private List<String> generateChristmasMap() {
		List<String> christ = new ArrayList<>();
		christ.add("The Seven Poor Travellers");
		christ.add("Blade-O'-Grass. Golden Grain. and Bread and Cheese and Kisses");
		christ.add("The Prodigal Village: A Christmas Tale");
		christ.add("The Potato Child & Others");
		christ.add("A Christmas Carol");
		Collections.sort(christ);
		return christ;

	}

	private List<String> generateGhostMap() {
		List<String> ghost = new ArrayList<>();
		ghost.add("The Jolly Corner");
		ghost.add("The Gateless Barrier");
		ghost.add("The Three Impostors- The Transmutations");
		ghost.add("The Garden of Survival");
		ghost.add("The Stoneground Ghost Tales");
		ghost.add("The Ghost of Guir House");
		Collections.sort(ghost);
		return ghost;
	}

	private List<String> generateWesternMap() {

		List<String> ws = new ArrayList<>();
		ws.add("The Heart of the Range");
		ws.add("Bucky O'Connor: A Tale of the Unfenced Border");
		ws.add("The Vision Splendid");
		ws.add("The Seventh Man");
		ws.add("Branded");
		ws.add("Bransford of Rainbow Range");
		ws.add("Paradise Bend");
		ws.add("The Mesa Trail");
		ws.add("The Ranchman");
		ws.add("The Trail-Hunter: A Tale of the Far West");
		ws.add("The Pirates of the Prairies: Adventures in the American Desert");
		ws.add("Justin Wingate- Ranchman");
		ws.add("The Prairie Flower: A Tale of the Indian Border");
		ws.add("The Red River Half-Breed: A Tale of the Wild North-West");
		ws.add("The Guide of the Desert");
		ws.add("Deadwood Dick Jr. Branded- Red Rover at Powder Pocket.");
		ws.add("White Wolf's Law: A Western Story");
		ws.add("Desert Gold");
		Collections.sort(ws);
		return ws;
	}

	private List<String> generateDetectiveMap() {
		List<String> detective = new ArrayList<>();

		detective.add("The Mystery of the Boule Cabinet: A Detective Story");
		detective.add("Elusive Isabel");
		detective.add("The Adventures of Jimmie Dale");
		detective.add("The Bradys and the Girl Smuggler Working for the Custom House");
		detective.add("The Vanished Messenger");
		detective.add("Other People's Money");
		detective.add("The Case of the Lamp That Went Out");
		detective.add("The Case of the Registered Letter");
		detective.add("The Case of the Pocket Diary Found in the Snow");
		detective.add("The Case of the Pool of Blood in the Pastor's Study");
		detective.add("The Case of the Golden Bullet");
		detective.add("The Yellow Crayon");
		detective.add("The Woman in the Alcove");
		detective.add("Initials Only");
		detective.add("A Millionaire of Yesterday");
		detective.add("The Bittermeads Mystery");
		detective.add("The House in the Mist");
		detective.add("Sight Unseen");
		detective.add("The Confession");
		detective.add("Havoc");
		detective.add("The Opal Serpent");
		detective.add("Simon");
		detective.add("Madeline Payne- the Detective's Daughter");
		detective.add("The Bradys Beyond Their Depth- The Great Swamp Mystery");
		detective.add("The Seven Secrets");
		detective.add("Anderson Crow- Detective");
		detective.add("Murder Point: A Tale of Keewatin");
		detective.add("The Count's Millions");
		detective.add("Ashton-Kirk- Criminologist");
		detective.add("Room Number 3- and Other Detective Stories");
		detective.add("An Artist in Crime");
		detective.add("Jacob's Ladder");
		detective.add("The Red Rat's Daughter");
		detective.add("The Green God");
		detective.add("X Y Z: A Detective Story");
		detective.add("The Hillman");
		detective.add("Whispering Wires");
		detective.add("A Mysterious Disappearance");
		detective.add("The Mynns' Mystery");
		detective.add("One of My Sons");
		detective.add("The Pagan's Cup");
		detective.add("£19-000");
		detective.add("The House of Strange Secrets: A Detective Story");
		detective.add("The House in the Mist");
		detective.add("The Solitary Farm");
		detective.add("Who?");
		detective.add("In the Onyx Lobby");
		detective.add("The Last Stroke: A Detective Story");
		detective.add("The Wicked Marquis");
		detective.add("The Amethyst Box");
		detective.add("Sharing Her Crime: A Novel");
		detective.add("The Black Eagle Mystery");
		detective.add("Mysterious Mr. Sabin");
		detective.add("The Black Star: A Detective Story");
		detective.add("Final Proof- The Value of Evidence");
		detective.add("The Red Mouse: A Mystery Romance");
		detective.add("Dangerous Ground- The Rival Detectives");
		detective.add("Cynthia Wakeham's Money");
		detective.add("The Crime Doctor");
		detective.add("The Gray Phantom's Return");
		detective.add("The Gray Phantom");
		detective.add("Out of a Labyrinth");
		detective.add("Mr. Marx's Secret");
		detective.add("A Gamble with Life");
		detective.add("Ashton-Kirk- Secret Agent");
		detective.add("The Crime and the Criminal");
		detective.add("Black Star's Campaign: A Detective Story");
		detective.add("The Red Lottery Ticket");
		detective.add("The King of Diamonds: A Tale of Mystery and Adventure");
		detective.add("Fighting Byng: A Novel of Mystery- Intrigue and Adventure");
		detective.add("The Sign of the Stranger");
		detective.add("The Broken Thread");
		detective.add("The House Opposite: A Mystery");
		detective.add("Quintus Oakes: A Detective Story");
		detective.add("The Barrel Mystery");
		detective.add("The Luminous Face");
		detective.add("The Mystery of the Clasped Hands: A Novel");
		detective.add("Great Porter Square: A Mystery. v. 1");
		detective.add("Great Porter Square: A Mystery. v. 2");
		detective.add("Great Porter Square: A Mystery. v. 3");
		detective.add("The Mark of Cain");
		detective.add("The Man Who Fell Through the Earth");
		detective.add("The Mystery Girl");
		detective.add("The Curved Blades");
		detective.add("Stolen Idols");
		detective.add("The Vanishing of Betty Varian");
		detective.add("The Room with the Tassels");
		detective.add("The Deep Lake Mystery");
		detective.add("Harry Blount- the Detective- The Martin Mystery Solved");
		detective.add("The Crime Club");
		detective.add("At War with Society- Tales of the Outcasts");
		detective.add("At the Villa Rose");
		detective.add("The Mystery of the Sycamore");
		detective.add("The Romance of Elaine - Sequel to 'Exploits of Elaine'");
		detective.add("The Exploits of Elaine");
		detective.add("A Siren");
		detective.add("Baron Trigault's Vengeance");
		detective.add("Average Jones");
		detective.add("The Ashiel mystery: A Detective Story");
		detective.add("Scarhaven Keep");
		detective.add("Return of Sherlock Holmes");
		detective.add("The Secret Adversary");
		detective.add("The Sign of Four");
		detective.add("The Works of Edgar Allan Poe I");
		detective.add("The Works of Edgar Allan Poe II");
		detective.add("The Works of Edgar Allan Poe III");
		detective.add("The Works of Edgar Allan Poe IV");
		detective.add("The Hound of the Baskervilles");
		detective.add("Tales of Terror and Mystery");
		detective.add("Memoirs of Shelock Holmes");
		detective.add("The Mysterious Affair at Styles");
		Collections.sort(detective);
		return detective;
	}

	private List<String> generateLiteraryMap() {

		List<String> bookList = new ArrayList<>();
		bookList.add("The Pupil");
		bookList.add("At Love's Cost");
		bookList.add("The Worshipper of the Image");
		bookList.add("The Book-Bills of Narcissus - An Account Rendered by Richard Le Gallienne");
		bookList.add("Joe Burke's Last Stand");
		bookList.add("O+F");
		bookList.add("Polly and the Princess");
		bookList.add("Maggie Miller: The Story of Old Hagar's Secret");
		bookList.add("The Obstacle Race");
		bookList.add("From out the Vasty Deep");
		bookList.add("Fate Knocks at the Door: A Novel");
		bookList.add("The Torrent (Entre Naranjos)");
		bookList.add("The Coxon Fund");
		bookList.add("The Mystery of Monastery Farm");
		bookList.add("Wife in Name Only");
		bookList.add("Darrel of the Blessed Isles");
		bookList.add("The Story of Bessie Costrell");
		bookList.add("Fated to Be Free: A Novel");
		bookList.add("Nancy: A Novel");
		bookList.add("The Top of the World");
		bookList.add("The Reason Why");
		bookList.add("Miriam Monfort - A Novel");
		bookList.add("A Perilous Secret");
		bookList.add("Deadham Hard: A Romance");
		bookList.add("A Simple Soul");
		bookList.add("A Spinner in the Sun");
		bookList.add("The Autobiography of a Slander");
		bookList.add("Tokyo to Tijuana: Gabriele Departing America");
		bookList.add("The Coquette's Victim - Everyday Life Library No. 1");
		bookList.add("The Song of the Blood-Red Flower");
		bookList.add("People Like That - A Novel");
		bookList.add("The Lady of Big Shanty");
		bookList.add("The Philanderers");
		bookList.add("Coralie - Everyday Life Library No. 2");
		bookList.add("Broken to the Plow - A Novel");
		bookList.add("The Wild Olive: A Novel");
		bookList.add("The American Baron: A Novel");
		bookList.add("Out of the Ashes");
		bookList.add("The Light That Lures");
		bookList.add("Miss Bretherton");
		bookList.add("Aylwin");
		bookList.add("The Collected Works of Ambrose Bierce- Volume 1");
		bookList.add("The Son of Clemenceau- A Novel of Modern Love and Life");
		bookList.add("One Day  A sequel to 'Three Weeks'");
		bookList.add("The Port of Missing Men");
		bookList.add("V. V.'s Eyes");
		bookList.add("If Winter Comes");
		bookList.add("Going into Society");
		bookList.add("Flames");
		bookList.add("Queed - A Novel");
		bookList.add("The Missing Bride");
		bookList.add("Septimus");
		bookList.add("The Exemplary Novels of Cervantes");
		bookList.add("True Love's Reward - A Sequel to Mona");
		bookList.add("The Doctor's Dilemma");
		bookList.add("A Daughter of To-Day");
		bookList.add("Sister Carmen");
		bookList.add("The Just and the Unjust");
		bookList.add("The Devil's Garden");
		bookList.add("Jaffery");
		bookList.add("Christian's Mistake");
		bookList.add("Vandover and the Brute");
		bookList.add("The Younger Set");
		bookList.add("The Man Thou Gavest");
		bookList.add("Red Pottage");
		bookList.add("The Keeper of the Door");
		bookList.add("The Cathedral");
		bookList.add("The Cab of the Sleeping Horse");
		bookList.add("The Jervaise Comedy");
		bookList.add("Sea and Shore - A Sequel to 'Miriam's Memoirs'");
		bookList.add("David Lockwin—The People's Idol");
		bookList.add("The Inner Sisterhood - A Social Study in High Colors");
		bookList.add("My Mother's Rival - Everyday Life Library No. 4");
		bookList.add("Marion Arleigh's Penance - Everyday Life Library No. 5");
		bookList.add("The Tragedy of the Chain Pier - Everyday Life Library No. 3");
		bookList.add("Lewie - The Bended Twig");
		bookList.add("Bessie's Fortune: A Novel");
		bookList.add("Gladys- the Reaper");
		bookList.add("The Baronet's Bride- A Woman's Vengeance");
		bookList.add("Tracy Park: A Novel");
		bookList.add("The Lever: A Novel");
		bookList.add("Trumps");
		bookList.add("Captivity");
		bookList.add("A Splendid Hazard");
		bookList.add("Far to Seek - A Romance of England and India");
		bookList.add("Bunker Bean");
		bookList.add("Ishmael- In the Depths");
		bookList.add("An Unpardonable Liar");
		bookList.add("The Seeker");
		bookList.add("Walter Harland - Memories of the Past");
		bookList.add("Garman and Worse: A Norwegian Novel");
		bookList.add("The Unseen Bridegroom- Wedded For a Week");
		bookList.add("His Excellency the Minister");
		bookList.add("An Englishwoman's Love-Letters");
		bookList.add("The Fatal Glove");
		bookList.add("The Lost Lady of Lone");
		bookList.add("Uncle Max");
		bookList.add("For Woman's Love");
		bookList.add("The Hoyden");
		bookList.add("The Cromptons");
		bookList.add("A Man and a Woman");
		bookList.add("Prince Fortunatus");
		bookList.add("Jan: A Dog and a Romance");
		bookList.add("Idolatry: A Romance");
		bookList.add("The Foolish Virgin");
		bookList.add("Ellen Walton - The Villain and His Victims");
		bookList.add("The Summons");
		bookList.add("What Necessity Knows");
		bookList.add("Led Astray and The Sphinx - Two Novellas In One Volume");
		bookList.add("The Measure of a Man");
		bookList.add("Foes");
		bookList.add("The Twins - A Domestic Novel");
		bookList.add("The Lee Shore");
		bookList.add("Bad Hugh");
		bookList.add("Eveline Mandeville - The Horse Thief Rival");
		bookList.add("Adrien Leroy");
		bookList.add("A Comedy of Masks: A Novel");
		bookList.add("The Husbands of Edith");
		bookList.add("Marzio's Crucifix and Zoroaster");
		bookList.add("Mike Fletcher: A Novel");
		bookList.add("Jacqueline of Golden River");
		bookList.add("My Little Lady");
		bookList.add("Elster's Folly: A Novel");
		bookList.add("Dangerous Ages");
		bookList.add("Lydia of the Pines");
		bookList.add("Mark Hurdlestone - The Two Brothers");
		bookList.add("The Halo");
		bookList.add("The Bad Man: A Novel");
		bookList.add("Miss Dexie - A Romance of the Provinces");
		bookList.add("The Man and the Moment");
		bookList.add("The Crock of Gold - A Rural Novel");
		bookList.add("Guy Livingstone'Thorough'");
		bookList.add("Destiny");
		bookList.add("The Actress in High Life - An Episode in Winter Quarters");
		bookList.add("Atlantis");
		bookList.add("Sunrise");
		bookList.add("Hearts and Masks");
		bookList.add("The Adventures of Kathlyn");
		bookList.add("The Thin Red Line and Blue Blood");
		bookList.add("The Second Honeymoon");
		bookList.add("The Red Seal");
		bookList.add("On the Church Steps");
		bookList.add("By the Light of the Soul: A Novel");
		bookList.add("Romance");
		bookList.add("Bella Donna: A Novel");
		bookList.add("The Courage of Marge O'Doone");
		bookList.add("Foes in Ambush");
		bookList.add("Contrary Mary");
		bookList.add("Betty at Fort Blizzard");
		bookList.add("Medoline Selwyn's Work");
		bookList.add("The Zeit-Geist");
		bookList.add("The Road to Mandalay - A Tale of Burma");
		bookList.add("Gentle Julia");
		bookList.add("Enter Bridget");
		bookList.add("The Lighted Match");
		bookList.add("A Melody in Silver");
		bookList.add("Parrot & Co.");
		bookList.add("The Treasure of Heaven: A Romance of Riches");
		bookList.add("The Place Beyond the Winds");
		bookList.add("A Court of Inquiry");
		bookList.add("A Mummer's Tale");
		bookList.add("The President: A Novel");
		bookList.add("The Poor Plutocrats");
		bookList.add("Dr. Dumany's Wife");
		bookList.add("Painted Windows");
		bookList.add("The House of Martha");
		bookList.add("Winner Take All");
		bookList.add("Then I'll Come Back to You");
		bookList.add("The Starbucks");
		bookList.add("Sword and Gown: A Novel");
		bookList.add("The Cow Puncher");
		bookList.add("Lady Larkspur");
		bookList.add("The Alchemist's Secret");
		bookList.add("Joyce of the North Woods");
		bookList.add("Michael McGrath- Postmaster");
		bookList.add("His Heart's Queen");
		bookList.add("Born Again");
		bookList.add("North of Fifty-Three");
		bookList.add("Kate Danton- Captain Danton's Daughters: A Novel");
		bookList.add("Dixie Hart");
		bookList.add("Heart - A Social Novel");
		bookList.add("A Political Romance");
		bookList.add("The Lovely Lady");
		bookList.add("A Son of the Hills");
		bookList.add("The Seventh Noon");
		bookList.add("The Triflers");
		bookList.add("Ernest Linwood- The Inner Life of the Author");
		bookList.add("The Complete Prose Works of Martin Farquhar Tupper");
		bookList.add("The Master-Knot of Human Fate");
		bookList.add("The Monctons: A Novel. Volume 1 (of 2)");
		bookList.add("Phyllis of Philistia");
		bookList.add("On the Stairs");
		bookList.add("April's Lady: A Novel");
		bookList.add("The Making of a Soul");
		bookList.add("The Tyranny of Weakness");
		bookList.add("Flint: His Faults- His Friendships and His Fortunes");
		bookList.add("Agatha's Husband: A Novel");
		bookList.add("The Mark Of Cain");
		bookList.add("The Daughters of Danaus");
		bookList.add("Afterwards");
		bookList.add("Luna Benamor");
		bookList.add("Embarrassments");
		bookList.add("The Pleasant Street Partnership: A Neighborhood Story");
		bookList.add("The Lure of the Mask");
		bookList.add("Aunt Rachel - A Rustic Sentimental Comedy");
		bookList.add("In Direst Peril");
		bookList.add("An Old Meerschaum - From Coals Of Fire And Other Stories- Volume II/III)");
		bookList.add("The Romance Of Giovanni Calvotti - From Coals Of Fire And Other Stories- Volume II/III)");
		bookList.add("Cruel Barbara Allen - From Coals Of Fire And Other Stories- Volume II/III)");
		bookList.add("Molly Bawn");
		bookList.add("Oswald Langdon - Pierre and Paul Lanier. A Romance of 1894-1898");
		bookList.add("Schwartz: A History - From 'Schwartz' by David Christie Murray");
		bookList.add("Young Mr. Barter's Repentance - From 'Schwartz' by David Christie Murray");
		bookList.add("Bulldog And Butterfly - From 'Schwartz' by David Christie Murray");
		bookList.add("Julia And Her Romeo: A Chronicle Of Castle Barfield - From 'Schwartz'");
		bookList.add("VC — A Chronicle of Castle Barfield and of the Crimea");
		bookList.add("Despair's Last Journey");
		bookList.add("The Tale Of Mr. Peter Brown - Chelsea Justice - From 'The New Decameron'|| Volume III.");
		bookList.add("Mary Louise and Josie O'Gorman");
		bookList.add("King Candaules");
		bookList.add("32 Caliber");
		bookList.add("Nell- of Shorne Mills- One Heart's Burden");
		bookList.add("A Modern Idyll");
		bookList.add("Gulmore- The Boss");
		bookList.add("Elder Conklin");
		bookList.add("Helen and Arthur- Miss Thusa's Spinning Wheel");
		bookList.add("Other People's Business: The Romantic Career of the Practical Miss Dale");
		bookList.add("Old Ebenezer");
		bookList.add("The Fête At Coqueville - 1907");
		bookList.add("Good Blood");
		bookList.add("Mistress Anne");
		bookList.add("The Folly Of Eustace - 1896");
		bookList.add("The Man Next Door");
		bookList.add("The Riddle Of The Rocks - 1895");
		bookList.add("The Phantoms Of The Foot-Bridge - 1895");
		bookList.add("The Moonshiners At Hoho-Hebee Falls - 1895");
		bookList.add("'way Down In Lonesome Cove - 1895");
		bookList.add("His 'Day In Court' - 1895");
		bookList.add("Flamsted quarries");
		bookList.add("The Dew of Their Youth");
		bookList.add("David Malcolm");
		bookList.add("Aladdin & Co.: A Romance of Yankee Magic");
		bookList.add("Cruel As The Grave");
		bookList.add("A True Friend: A Novel");
		bookList.add("The Book of All-Power");
		bookList.add("The Kingdom Round the Corner: A Novel");
		bookList.add("An Apostate: Nawin of Thais");
		bookList.add("Her Mother's Secret");
		bookList.add("Margarita's Soul: The Romantic Recollections of a Man of Fifty");
		bookList.add("Burlesques");
		bookList.add("Ringfield: A Novel");
		bookList.add("A Red Wallflower");
		bookList.add("A Pessimist in Theory and Practice");
		bookList.add("Bird of Paradise");
		bookList.add("The Giant's Robe");
		bookList.add("Five O'Clock Tea: Farce");
		bookList.add("A Bachelor's Dream");
		bookList.add("The Dominant Dollar");
		bookList.add("A Romantic Young Lady");
		bookList.add("The Beloved Woman");
		bookList.add("In Brief Authority");
		bookList.add("Not Like Other Girls");
		bookList.add("The Limit");
		bookList.add("The Brass Bound Box");
		bookList.add("Wee Wifie");
		bookList.add("Lover or Friend");
		bookList.add("Chit-Chat  Nirvana  The Searchlight");
		bookList.add("Cleo The Magnificent- The Muse of the Real: A Novel");
		bookList.add("The First Violin - A Novel");
		bookList.add("People of Position");
		bookList.add("Australia Revenged");
		bookList.add("The Prisoner");
		bookList.add("Rope");
		bookList.add("Once to Every Man");
		bookList.add("Mixed Faces");
		bookList.add("Nobody");
		bookList.add("The Hound From The North");
		bookList.add("The Heart of Thunder Mountain");
		bookList.add("Hetty's Strange History");
		bookList.add("The Princess Virginia");
		bookList.add("The Strollers");
		bookList.add("Victor's Triumph - Sequel to A Beautiful Fiend");
		bookList.add("The Missionary");
		bookList.add("The Gorgeous Girl");
		bookList.add("The Dominant Strain");
		bookList.add("Audrey Craven");
		bookList.add("The Man Who Wins");
		bookList.add("The Paliser case");
		bookList.add("Dwellers in the Hills");
		bookList.add("A Great Man: A Frolic");
		bookList.add("The Old Countess- The Two Proposals");
		bookList.add("Love and Lucy");
		bookList.add("The Crimson Tide: A Novel");
		bookList.add("The Doctor's Family");
		bookList.add("The Rector");
		bookList.add("Changing Winds - A Novel");
		bookList.add("Jan and Her Job");
		bookList.add("The Eye of Dread");
		bookList.add("In the Shadow of the Hills");
		bookList.add("Jessica- the Heiress");
		bookList.add("Our Next-Door Neighbors");
		bookList.add("Amaryllis at the Fair");
		bookList.add("Young Barbarians");
		bookList.add("Robinetta");
		bookList.add("At the Crossroads");
		bookList.add("The Vast Abyss - The Story of Tom Blount- his Uncles and his Cousin Sam");
		bookList.add("The Quality of Mercy");
		bookList.add("Name and Fame: A Novel");
		bookList.add("A Noble Woman");
		bookList.add("Tante");
		bookList.add("A Manual of the Art of Fiction");
		bookList.add("East of the Shadows");
		bookList.add("Officer 666");
		bookList.add("Phemie Frost's Experiences");
		bookList.add("Mabel's Mistake");
		bookList.add("A Captain in the Ranks: A Romance of Affairs");
		bookList.add("The Phantom Lover");
		bookList.add("The Benefactress");
		bookList.add("Hope Mills- Between Friend and Sweetheart");
		bookList.add("The Lieutenant-Governor: A Novel");
		bookList.add("A Manifest Destiny");
		bookList.add("Wild Oranges");
		bookList.add("Outside Inn");
		bookList.add("The Pines of Lory");
		bookList.add("A Danish Parsonage");
		bookList.add("Wings of the Wind");
		bookList.add("In the Heart of a Fool");
		bookList.add("Anything Once");
		bookList.add("An Ocean Tramp");
		bookList.add("Daisy's Necklace- and What Came of It");
		bookList.add("Sir Tom");
		bookList.add("The Record of Nicholas Freydon - An Autobiography");
		bookList.add("Captain Pott's Minister");
		bookList.add("Absolution");
		bookList.add("The Fate of Felix Brand");
		bookList.add("Clark's Field");
		bookList.add("Materials and Methods of Fiction - With an Introduction by Brander Matthews");
		bookList.add("The Beggar Man");
		bookList.add("Ten Thousand a-Year. Volume 1.");
		bookList.add("Aliens");
		bookList.add("Cupid's Middleman");
		bookList.add("Walladmor- Vol. 1 (of 2)");
		bookList.add("Walladmor- Vol. 2 (of 2)");
		bookList.add("The Serapion Brethren- Vol. II");
		bookList.add("Checkers: A Hard-luck Story");
		bookList.add("The Serapion Brethren- Vol. I.");
		bookList.add("Portia- By Passions Rocked");
		bookList.add("The Portal of Dreams");
		bookList.add("A Life Sentence: A Novel");
		bookList.add("Captain Macedoine's Daughter");
		bookList.add("A Rent In A Cloud");
		bookList.add("Rose MacLeod");
		bookList.add("When the Cock Crows");
		bookList.add("Long Live the King!");
		bookList.add("Arthur O'Leary: His Wanderings And Ponderings In Many Lands");
		bookList.add("The Brightener");
		bookList.add("A Novelist on Novels");
		bookList.add("A Day's Ride: A Life's Romance");
		bookList.add("That Boy Of Norcott's");
		bookList.add("Tried for Her Life - A Sequel to 'Cruel As the Grave'");
		bookList.add("Fairfax and His Pride: A Novel");
		bookList.add("One Of Them");
		bookList.add("Lady Cassandra");
		bookList.add("Sir Hilton's Sin");
		bookList.add("The Sapphire Cross");
		bookList.add("Thereby Hangs a Tale. Volume One");
		bookList.add("An Unknown Lover");
		bookList.add("Sir Jasper Carew: His Life and Experience");
		bookList.add("The Truth About Tristrem Varick: A Novel");
		bookList.add("Was It Right to Forgive? A Domestic Romance");
		bookList.add("Faith and Unfaith: A Novel");
		bookList.add("The Little Schoolmaster Mark: A Spiritual Romance");
		bookList.add("A Man in the Open");
		bookList.add("Roland Cashel- Volume I (of II)");
		bookList.add("Roland Cashel- Volume II (of II)");
		bookList.add("The Furnace");
		bookList.add("The Gambler: A Novel");
		bookList.add("The Fortunes Of Glencore");
		bookList.add("The Bachelors: A Novel");
		bookList.add("Tony Butler");
		bookList.add("Norine's Revenge- and- Sir Noel's Heir");
		bookList.add("The Cottage of Delight: A Novel");
		bookList.add("An Engagement of Convenience: A Novel");
		bookList.add("The Key to Yesterday");
		bookList.add("The Haunted Pajamas");
		bookList.add("King of Camargue");
		bookList.add("Regina- or the Sins of the Fathers");
		bookList.add("The Reclaimers");
		bookList.add("The Mysterious Wanderer- Vol. I");
		bookList.add("Love Works Wonders: A Novel");
		bookList.add("The Belovéd Traitor");
		bookList.add("A Poached Peerage");
		bookList.add("Lady Maude's Mania");
		bookList.add("A Double Knot");
		bookList.add("By Birth a Lady");
		bookList.add("A Blot on the Scutcheon");
		bookList.add("Capricious Caroline");
		bookList.add("Zula");
		bookList.add("The Girl From Tim's Place");
		bookList.add("The Star-Gazers");
		bookList.add("Of High Descent");
		bookList.add("The Man with a Shadow");
		bookList.add("Phases of an Inferior Planet");
		bookList.add("For the Allinson Honor");
		bookList.add("Alas! A Novel");
		bookList.add("The Rosery Folk");
		bookList.add("The Gold Brick");
		bookList.add("The Triumph of Virginia Dale");
		bookList.add("Mrs. Halliburton's Troubles");
		bookList.add("Through Night to Light: A Novel");
		bookList.add("What the Swallow Sang: A Novel");
		bookList.add("King of the Castle");
		bookList.add("Barren Honour: A Novel");
		bookList.add("The Mysterious Wanderer  Vol. II");
		bookList.add("The Breaking of the Storm- Vol. I.");
		bookList.add("The Breaking of the Storm- Vol. II.");
		bookList.add("The Breaking of the Storm- Vol. III.");
		bookList.add("Willing to Die: A Novel");
		bookList.add("Commodore Junk");
		bookList.add("The Man with the Double Heart");
		bookList.add("The Ivory Gate- a new edition");
		bookList.add("Problematic Characters: A Novel");
		bookList.add("Englefield Grange- Mary Armstrong's Troubles");
		bookList.add("The Rival Crusoes- The Ship Wreck - Also A Voyage to Norway  and The Fisherman's Cottage.");
		bookList.add("Mystery and Confidence: A Tale. Vol. 1");
		bookList.add("Mystery and Confidence: A Tale. Vol. 3");
		bookList.add("Mystery and Confidence: A Tale. Vol. 2");
		bookList.add("The Count of Nideck - adapted from the French of Erckmann-Chartrian");
		bookList.add("The Carleton Case");
		bookList.add("Dilemmas of Pride- (Vol 2 of 3)");
		bookList.add("Dilemmas of Pride- (Vol 3 of 3)");
		bookList.add("Girl Alone");
		bookList.add("The Blind Mother- and The Last Confession");
		bookList.add("In the Van- The Builders");
		bookList.add("The Guerilla Chief- and Other Tales");
		bookList.add("The Oyster");
		bookList.add("The Streets of Ascalon - Episodes in the Unfinished Career of Richard Quarren- Esqre.");
		bookList.add("A Wife's Duty: A Tale");
		bookList.add("Sir Brook Fossbrooke- Volume I.");
		bookList.add("Sir Brook Fossbrooke- Volume II.");
		bookList.add("A Practical Novelist");
		bookList.add("The Long Lane's Turning");
		bookList.add("The Lady Evelyn: A Story of To-day");
		bookList.add("Mad: A Story of Dust and Ashes");
		bookList.add("Friends I Have Made");
		bookList.add("Mrs. Geoffrey");
		bookList.add("A Charming Fellow- Volume I");
		bookList.add("A Charming Fellow- Volume II");
		bookList.add("A Charming Fellow- Volume III");
		bookList.add("The Dodd Family Abroad- Vol. I");
		bookList.add("The Dodd Family Abroad- Vol. II");
		bookList.add("The Golden Road");
		bookList.add("Only One Love- Who Was the Heir");
		bookList.add("The Haute Noblesse: A Novel");
		bookList.add("The Haunted Room: A Tale");
		bookList.add("Doctor Cupid: A Novel");
		bookList.add("The Funny Philosophers- or Wags and Sweethearts.  A Novel");
		bookList.add("Titan: A Romance. v. 1 (of 2)");
		bookList.add("The Late Tenant");
		bookList.add("The Game and the Candle");
		bookList.add("Lily Pearl and The Mistress of Rosedale");
		bookList.add("Dilemmas of Pride- (Vol 1 of 3)");
		bookList.add("The Hills of Refuge: A Novel");
		bookList.add("The Finger of Fate: A Romance");
		bookList.add("The Child Wife");
		bookList.add("Silent Struggles");
		bookList.add("A Speckled Bird");
		bookList.add("Hesperus- Forty-Five Dog-Post-Days: A Biography. Vol. I.");
		bookList.add("Hesperus- Forty-Five Dog-Post-Days: A Biography. Vol. II.");
		bookList.add("Bijou");
		bookList.add("The Shooting of Dan McGrew- A Novel. Based on the Famous Poem of Robert Service");
		bookList.add("The Invisible Lodge");
		bookList.add("Faithful Margaret: A Novel");
		bookList.add("Zoe- Some Day: A Novel");
		bookList.add("Wives and Widows- The Broken Life");
		bookList.add("The Turn of the Tide: The Story of How Margaret Solved Her Problem");
		bookList.add("Titan: A Romance. v. 2 (of 2)");
		bookList.add("Jessamine: A Novel");
		bookList.add("The Story of an Untold Love");
		bookList.add("A Butterfly on the Wheel: A Novel");
		bookList.add("Fordham's Feud");
		bookList.add("The Haunted Homestead: A Novel");
		bookList.add("One Maid's Mischief");
		bookList.add("Midnight Webs");
		bookList.add("A Life For a Love: A Novel");
		bookList.add("The Wayfarers");
		bookList.add("Pray You- Sir- Whose Daughter?");
		bookList.add("The Empty Sack");
		bookList.add("Rough-Hewn");
		bookList.add("Daisy Thornton");
		bookList.add("Jessie Graham");
		bookList.add("The Debit Account");
		bookList.add("The Sins of the Children: A Novel");
		bookList.add("Notwithstanding");
		bookList.add("The Open Question: A Tale of Two Temperaments");
		bookList.add("The Story of Louie");
		bookList.add("A Humble Enterprise");
		bookList.add("In Accordance with the Evidence");
		bookList.add("The Heatherford Fortune - a sequel to the Magic Cameo");
		bookList.add("The Sailor");
		bookList.add("Kenneth McAlpine: A Tale of Mountain- Moorland and Sea");
		bookList.add("The Making of William Edwards- The Story of the Bridge of Beauty");
		bookList.add("A Witch of the Hills- v. 1 [of 2]");
		bookList.add("A Witch of the Hills- v. 2 [of 2]");
		bookList.add("The Happy Warrior");
		bookList.add("A Knight on Wheels");
		bookList.add("Loaded Dice");
		bookList.add("Gargoyles");
		bookList.add("The Story of Charles Strange: A Novel. Vol. 1 (of 3)");
		bookList.add("The Story of Charles Strange: A Novel. Vol. 2 (of 3)");
		bookList.add("The Story of Charles Strange: A Novel. Vol. 3 (of 3)");
		bookList.add("Miranda of the Balcony: A Story");
		bookList.add("The Truants");
		bookList.add("The Turnstile");
		bookList.add("The Maker of Opportunities");
		bookList.add("A Singular Metamorphosis");
		bookList.add("The Monctons: A Novel. Volume 2 (of 2)");
		bookList.add("The White Blackbird");
		bookList.add("Miser Farebrother: A Novel (vol. 1 of 3)");
		bookList.add("It May Be True- Vol. 2 (of 3)");
		bookList.add("It May Be True- Vol. 3 (of 3)");
		bookList.add("Tales for Fifteen");
		bookList.add("Mitchelhurst Place: A Novel. Vol. 1 (of 2)");
		bookList.add("Wyndham's Pal");
		bookList.add("Mabel: A Novel. Vol. 3 (of 3)");
		bookList.add("Mildred Arkell: A Novel. Vol. 2 (of 3)");
		bookList.add("The Mysterious Wanderer- Vol. III - A Novel in Three Volumes");
		bookList.add("Guy and Pauline");
		bookList.add("Sylvia & Michael: The later adventures of Sylvia Scarlett");
		bookList.add("Mildred Arkell: A Novel. Vol. 1 (of 3)");
		bookList.add("Mildred Arkell: A Novel. Vol. 3 (of 3)");
		bookList.add("The Marriage of Esther");
		bookList.add("Vassall Morton: A Novel");
		bookList.add("A Sovereign Remedy");
		bookList.add("In Silk Attire: A Novel");
		bookList.add("Fashion and Famine");
		bookList.add("A Woman's Love");
		bookList.add("Mabel: A Novel. Vol. 2 (of 3)");
		bookList.add("The Threatening Eye");
		bookList.add("True to a Type- Vol. 1 (of 2)");
		bookList.add("True to a Type- Vol. 2 (of 2)");
		bookList.add("The Coward Behind the Curtain");
		bookList.add("It May Be True- Vol. 1 (of 3)");
		bookList.add("Miss Hildreth: A Novel- Volume 1");
		bookList.add("Miss Hildreth: A Novel- Volume 2");
		bookList.add("Miss Hildreth: A Novel- Volume 3");
		bookList.add("Violet Forster's Lover");
		bookList.add("Under One Flag");
		bookList.add("The Twickenham Peerage");
		bookList.add("The Ordeal of Elizabeth");
		bookList.add("The Splendid Fairing");
		bookList.add("Moth and Rust Together with Geoffrey's Wife and The Pitfall");
		bookList.add("Prince Charlie");
		bookList.add("The Hypocrite");
		bookList.add("The White Virgin");
		bookList.add("The Tiger Lily");
		bookList.add("Sawn Off: A Tale of a Family Tree");
		bookList.add("Gretchen: A Novel");
		bookList.add("The Adventures of Peregrine Pickle");
		bookList.add("Meg- of Valencia");
		bookList.add("The Day of Temptation");
		bookList.add("Stolen Souls");
		bookList.add("Rayton: A Backwoods Mystery");
		bookList.add("Jenifer's Prayer");
		bookList.add("The Eve of All-Hallows- Adelaide of Tyrconnel- v. 1 of 3");
		bookList.add("Ten Thousand a-Year. Volume 3.");
		bookList.add("The Arm-Chair at the Inn");
		bookList.add("Mrs. Dorriman: A Novel. Volume 1 of 3");
		bookList.add("Mrs. Dorriman: A Novel. Volume 2 of 3");
		bookList.add("Mrs. Dorriman: A Novel. Volume 3 of 3");
		bookList.add("Ten Thousand a-Year. Volume 2.");
		bookList.add("Mount Royal: A Novel. Volume 1 of 3");
		bookList.add("Mount Royal: A Novel. Volume 2 of 3");
		bookList.add("Mount Royal: A Novel. Volume 3 of 3");
		bookList.add("Mohawks: A Novel. Volume 1 of 3");
		bookList.add("Mohawks: A Novel. Volume 2 of 3");
		bookList.add("Mohawks: A Novel. Volume 3 of 3");
		bookList.add("The Yazoo Mystery: A Novel");
		bookList.add("The Decadent: Being the Gospel of Inaction");
		bookList.add("Rose Clark");
		bookList.add("Mabel: A Novel. Vol. 1 (of 3)");
		bookList.add("A Fortnight of Folly");
		bookList.add("A Changed Heart: A Novel");
		bookList.add("Albrecht");
		bookList.add("The Last Miracle");
		bookList.add("John Marvel- Assistant");
		bookList.add("The Golden Bough");
		bookList.add("The Terms of Surrender");
		bookList.add("Ragna");
		bookList.add("A Fair Mystery: The Story of a Coquette");
		bookList.add("The Debatable Land: A Novel");
		bookList.add("The Eve of All-Hallows- Adelaide of Tyrconnel- v. 2 of 3");
		bookList.add("The World Before Them: A Novel. Volume 2 (of 3)");
		bookList.add("The World Before Them: A Novel. Volume 1 (of 3)");
		bookList.add("The World Before Them: A Novel. Volume 3 (of 3)");
		bookList.add("The Shadow of a Sin");
		bookList.add("Barbara Rebell");
		bookList.add("Lady Eureka- The Mystery: A Prophecy of the Future. Volume 1");
		bookList.add("Lady Eureka- The Mystery: A Prophecy of the Future. Volume 2");
		bookList.add("Lady Eureka- The Mystery: A Prophecy of the Future. Volume 3");
		bookList.add("Miracle Gold: A Novel (Vol. 2 of 3)");
		bookList.add("Miracle Gold: A Novel (Vol. 1 of 3)");
		bookList.add("Miracle Gold: A Novel (Vol. 3 of 3)");
		bookList.add("Blackthorn Farm");
		bookList.add("Narcissus");
		bookList.add("The Narrow House");
		bookList.add("Yonder");
		bookList.add("The Deacon: An Original Comedy Drama in Five Acts");
		bookList.add("Tales and Fantasies");
		bookList.add("Tempest-Driven: A Romance (Vol. 1 of 3)");
		bookList.add("Tempest-Driven: A Romance (Vol. 2 of 3)");
		bookList.add("Tempest-Driven: A Romance (Vol. 3 of 3)");
		bookList.add("Swords Reluctant");
		bookList.add("It Pays to Smile");
		bookList.add("The Last of Their Race");
		bookList.add("Guy Kenmore's Wife- and The Rose and the Lily");
		bookList.add("Devota");
		bookList.add("Hugh Crichton's Romance");
		bookList.add("Mrs. Severn: A Novel- Vol. 1 (of 3)");
		bookList.add("The Gypsy Queen's Vow");
		bookList.add("Flora Adair- Love Works Wonders. Vol. 1 (of 2)");
		bookList.add("Flora Adair- Love Works Wonders. Vol. 2 (of 2)");
		bookList.add("Munster Village");
		bookList.add("First Love: A Novel. Vol. 1 of 3");
		bookList.add("The Eve of All-Hallows- Adelaide of Tyrconnel- v. 3 of 3");
		bookList.add("Wanted: A Husband. A Novel");
		bookList.add("The Rebel Chief: A Tale of Guerilla Life");
		bookList.add("Held to Answer: A Novel");
		bookList.add("Little Golden's Daughter- The Dream of a Life Time");
		bookList.add("Kathleen's Diamonds- She Loved a Handsome Actor");
		bookList.add("Wild Margaret");
		bookList.add("A Man's World");
		bookList.add("A Mock Idyl");
		bookList.add("Farewell");
		bookList.add("The Insurgent Chief");
		bookList.add("The Flying Horseman");
		bookList.add("The House on the Moor- v. 1-3");
		bookList.add("The House on the Moor- v. 2-3");
		bookList.add("The House on the Moor- v. 3-3");
		bookList.add("Pretty Geraldine- the New York Salesgirl- Wedded to Her Choice");
		bookList.add("The Romantic Lady");
		bookList.add("The Good Time Coming");
		bookList.add("Uncle William: The Man Who Was Shif'less");
		bookList.add("Alone");
		bookList.add("Outpost");
		bookList.add("The Idol of the Blind: A Novel");
		bookList.add("A Country Sweetheart");
		bookList.add("When Egypt Went Broke: A Novel");
		bookList.add("Kate Vernon: A Tale. Vol. 1 (of 3)");
		bookList.add("The Fiction Factory");
		bookList.add("Odette's Marriage - A Novel- from the French of Albert Delpit");
		bookList.add("Sekhet");
		bookList.add("Mirk Abbey- Volume 1 (of 3)");
		bookList.add("Mirk Abbey- Volume 2 (of 3)");
		bookList.add("Mirk Abbey- Volume 3 (of 3)");
		bookList.add("Half a Rogue");
		bookList.add("Dawn");
		bookList.add("The Circassian Slave- the Sultan's favorite : a story of Constantinople and the Caucasus");
		bookList.add("A Life for a Life- Volume 1 (of 3)");
		bookList.add("A Life for a Life- Volume 2 (of 3)");
		bookList.add("A Life for a Life- Volume 3 (of 3)");
		bookList.add("Tinman");
		bookList.add("Hilda Wade- a Woman with Tenacity of Purpose");
		bookList.add("King Midas: a Romance");
		bookList.add("The Spider and the Fly- An Undesired Love");
		bookList.add("Leah Mordecai: A Novel");
		bookList.add("Chetwynd Calverley - New Edition- 1877");
		bookList.add("The Desultory Man - Collection of Ancient and Modern British Novels and Romances");
		bookList.add("The Forgery- Best Intentions.");
		bookList.add("Leslie's Loyalty");
		bookList.add("De L'Orme. - The Works of G. P. R. James- Esq.|| Vol. XVI.");
		bookList.add("Morley Ernstein- the Tenants of the Heart");
		bookList.add("The Fate: A Tale of Stirring Times");
		bookList.add("Margret Howth: A Story of To-day");
		bookList.add("Delaware- The Ruined Family. Vol. 1");
		bookList.add("Delaware- The Ruined Family. Vol. 2");
		bookList.add("Delaware- The Ruined Family. Vol. 3");
		bookList.add("A Whim- and Its Consequences - Collection of British Authors");
		bookList.add("Corpus of a Siam Mosquito");
		bookList.add("Daireen. Volume 1 of 2");
		bookList.add("Daireen. Volume 2 of 2");
		bookList.add("Daireen. Complete");
		bookList.add("Priscilla and Charybdis: A Story of Alternatives");
		bookList.add("Well- After All");
		bookList.add("The Confessions of Harry Lorrequer — Volume 1");
		bookList.add("The Confessions of Harry Lorrequer — Volume 2");
		bookList.add("The Confessions of Harry Lorrequer — Volume 3");
		bookList.add("The Confessions of Harry Lorrequer — Volume 4");
		bookList.add("The Confessions of Harry Lorrequer — Volume 5");
		bookList.add("The Confessions of Harry Lorrequer — Volume 6");
		bookList.add("The Confessions of Harry Lorrequer — Complete");
		bookList.add("Kate Vernon: A Tale. Vol. 2 (of 3)");
		bookList.add("The Touch of Abner");
		bookList.add("Kate Vernon: A Tale. Vol. 3 (of 3)");
		bookList.add("At Last: A Novel");
		bookList.add("The Prose of Alfred Lichtenstein");
		bookList.add("The Queen of Sheba- and My Cousin the Colonel");
		bookList.add("The Flyers");
		bookList.add("Michael's Crag");
		bookList.add("Celibates");
		bookList.add("Opening a Chestnut Burr");
		bookList.add("Fran");
		bookList.add("Philistia");
		bookList.add("From Jest to Earnest");
		bookList.add("No Defense- Volume 1.");
		bookList.add("No Defense- Volume 2.");
		bookList.add("No Defense- Volume 3.");
		bookList.add("No Defense- Complete");
		bookList.add("Carnac's Folly- Volume 1.");
		bookList.add("Carnac's Folly- Volume 2.");
		bookList.add("Carnac's Folly- Volume 3.");
		bookList.add("Carnac's Folly- Complete");
		bookList.add("A Knight of the Nineteenth Century");
		bookList.add("Self-Raised- From the Depths");
		bookList.add("The Iron Woman");
		bookList.add("The Grey Lady");
		bookList.add("Any Coincidence Is - The Day Julia & Cecil the Cat Faced a Fate Worse Than Death");
		bookList.add("Si'Wren of the Patriarchs");
		bookList.add("The Doctor's Daughter");
		bookList.add("Creatures That Once Were Men");
		bookList.add("The Happy Adventurers");
		bookList.add("Aikenside");
		bookList.add("A Terrible Secret: A Novel");
		bookList.add("Fanshawe");
		bookList.add("The Dolliver Romance");
		bookList.add("Linda Condon");
		bookList.add("Doctor Grimshawe's Secret — a Romance");
		bookList.add("A Psychological Counter-Current in Recent Fiction");
		bookList.add("The Wishing-Ring Man");
		bookList.add("Falkland- Book 1.");
		bookList.add("Falkland- Book 2.");
		bookList.add("Falkland- Book 3.");
		bookList.add("Falkland- Book 4.");
		bookList.add("Falkland- Complete");
		bookList.add("Fan : The Story of a Young Girl's Life");
		bookList.add("The Rise of Iskander");
		bookList.add("The Vision of Desire");
		bookList.add("An Ambitious Man");
		bookList.add("A Fool for Love");
		bookList.add("A Fountain Sealed");
		bookList.add("The Misses Mallett (The Bridge Dividing)");
		bookList.add("Monsieur Maurice");
		bookList.add("Ptomaine Street: The Tale of Warble Petticoat");
		bookList.add("Young People's Pride: A Novel");
		bookList.add("Rest Harrow: A Comedy of Resolution");
		bookList.add("The Puritans");
		bookList.add("The Woman with the Fan");
		bookList.add("The Far Horizon");
		bookList.add("The Philistines");
		bookList.add("The Wheel O' Fortune");
		bookList.add("The Pagans");
		bookList.add("The Living Link: A Novel");
		bookList.add("The Brass Bowl");
		bookList.add("From One Generation to Another");
		bookList.add("Miss Theodosia's Heartstrings");
		bookList.add("A Love Story");
		bookList.add("The Golden Calf");
		bookList.add("Eleanor");
		bookList.add("The Bride of Dreams");
		bookList.add("Henry Dunbar: A Novel");
		bookList.add("Charlotte's Inheritance");
		bookList.add("One Day's Courtship- and The Heralds of Fame");
		bookList.add("Hetty's Strange History");
		bookList.add("Birds of Prey");
		bookList.add("A Woman Intervenes");
		bookList.add("Theresa Marchmont- the Maid of Honour: A Tale");
		bookList.add("Helen of the Old House");
		bookList.add("The Fortune Hunter");
		bookList.add("The Price of Things");
		bookList.add("The Two Guardians - Home in This World");
		bookList.add("Mr. Waddington of Wyck");
		bookList.add("Captivating Mary Carstairs");
		bookList.add("Persuasion");
		bookList.add("Alice's Adventures in Wonderland");
		bookList.add("Through the Looking-Glass");
		bookList.add("Pride and Prejudice");
		bookList.add("Great Expectations");
		bookList.add("Mansfield Park");
		bookList.add("Emma");
		bookList.add("Sense and Sensibility");
		bookList.add("The Confidence-Man");
		bookList.add("Five Tales");
		bookList.add("A Tangled Tale");
		bookList.add("Justice");
		bookList.add("The Little Man");
		bookList.add("Pierre or The Ambiguities");
		bookList.add("The Forsyte Saga");
		bookList.add("The Game of Logic");
		bookList.add("Loyalties");
		bookList.add("Sylvie and Bruno");
		bookList.add("Oliver Twist");
		bookList.add("David Copperfield");
		bookList.add("Hard Times");
		bookList.add("A Tale of Two Cities");
		Collections.sort(bookList);
		return bookList;
	}

	private List<String> generateAll() {
		List<String> all = new ArrayList<>();
		all.addAll(generateAllegoryMap());
		all.addAll(generateChristmasMap());
		all.addAll(generateDetectiveMap());
		all.addAll(generateGhostMap());
		all.addAll(generateHumourMap());
		all.addAll(generateLiteraryMap());
		all.addAll(generateLoveMap());
		all.addAll(generateSeaMap());
		all.addAll(generateWesternMap());
		Collections.sort(all);
		return all;

	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getBook() {
		return book;
	}

	public void setBook(String book) {
		this.book = book;
	}

	public Map<String, String> getGenreMap() {
		return genreMap;
	}

	public void setGenreMap(Map<String, String> genreMap) {
		this.genreMap = genreMap;
	}

	public List<String> getBooks() {
		return books;
	}

	public void setBooks(List<String> books) {
		this.books = books;
	}

	public void setData(Map<String, List<String>> data) {
		this.data = data;
	}

	public String getTxt() {
		return txt;
	}

	public void setTxt(String txt) {
		this.txt = txt;
	}

	public List<String> completeText(String query) {
		System.out.println("query" + query);
		List<String> allBooks = books;
		System.out.println("all" + allBooks.size());
		List<String> filtered = new ArrayList<>();
		for (String val : books) {
			if (val.toLowerCase().contains(query.toLowerCase())) {
				filtered.add(val);
			}
		}
		System.out.println(filtered.size() + " filtered");
		return filtered;
	}

	public String getSelBook() {
		return selBook;
	}

	public void setSelBook(String selBook) {
		this.selBook = selBook;
	}

	public boolean isShallShowTable() {
		return shallShowTable;
	}

	public void setShallShowTable(boolean shallShowTable) {
		this.shallShowTable = shallShowTable;
	}

	public CarService getService() {
		return service;
	}

	public void setService(CarService service) {
		this.service = service;
	}

	public List<BookUI> getSimBooks() {
		return simBooks;
	}

	public void setSimBooks(List<BookUI> simBooks) {
		this.simBooks = simBooks;
	}

	public String getShowMsg() {
		return showMsg;
	}

	public void setShowMsg(String showMsg) {
		this.showMsg = showMsg;
	}

	public void download() throws IOException {
		FRWebUtils utils = new FRWebUtils();
		Map<String, String> book_master = utils.getAllMasterBooks(); // key = bookId, Value = Book_Name
		System.out.println(book);
		String bookid = utils.getMasterBookId(book_master, book);
		System.out.println(bookid);
		// InputStream stream =
		// FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/epub/"+bookid);
		// file = new DefaultStreamedContent(stream, "application/epub", bookid);

		FacesContext fc = FacesContext.getCurrentInstance();
		ExternalContext ec = fc.getExternalContext();
		ec.responseReset();
		ec.setResponseContentType("application/epub");
		ec.setResponseContentLength(exportContent.length);
		String attachmentName = "attachment; filename=\"" + bookid + "\"";
		ec.setResponseHeader("Content-Disposition", attachmentName);
		try {
			OutputStream output = ec.getResponseOutputStream();
			Streams.copy(new ByteArrayInputStream(exportContent), output, false);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		fc.responseComplete();

	}

	public StreamedContent getFile() {
		return file;
	}

	public void setFile(StreamedContent file) {
		this.file = file;
	}

}