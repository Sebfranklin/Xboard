package com.example.engine

data class EmojiEntry(
    val glyph: String,
    val category: EmojiCategory,
    val keywords: List<String>
)

enum class EmojiCategory(val label: String, val iconGlyph: String) {
    SMILEYS("Faces", "😀"),
    GESTURES("Hands", "👍"),
    WORK_TECH("Work", "💻"),
    NATURE_FOOD("Life", "☕"),
    SYMBOLS("Marks", "✨")
}

object BuiltInLexicon {

    // Ordered roughly by Zipf frequency tiers; score derived from index & tier.
    val coreWords: List<Pair<String, Int>> by lazy {
        val raw = """
            the be to of and a in that have i it for not on with he as you do at this but his by from they we say her she or an will my one all would there their what so up out if about who get which go me when make can like time no just him know take people into year your good some could them see other than then now look only come its over think also back after use two how our work first well way even new want because any these give day most us
            is are was were been being am had has did does doing done can't don't won't didn't doesn't isn't aren't wasn't weren't haven't hasn't hadn't wouldn't shouldn't couldn't i'm you're he's she's it's we're they're i've you've we've they've i'll you'll he'll she'll we'll they'll i'd you'd he'd she'd we'd they'd that's there's here's what's who's where's when's why's how's let's
            very much more many before through between under while where why again further once here both each few own same too such why how
            hello hi hey thanks thank please sorry welcome morning afternoon evening night goodbye bye okay ok sure yes yeah yep nope maybe
            today tomorrow yesterday tonight week month year minute second hour moment soon later early late always never often sometimes usually
            need feel seem try leave call keep let begin help talk turn start show hear play run move live believe hold bring happen write provide sit stand lose pay meet include continue set learn change lead understand watch follow stop create speak read allow add spend grow open walk win offer remember love consider appear buy wait serve die send expect build stay fall cut reach kill remain suggest raise pass sell require report decide pull
            great good better best bad worse worst nice awesome amazing cool fine happy glad excited tired busy ready free full empty clean clear easy hard simple complex fast quick slow high low long short big small large little old young right wrong true false real local offline smart fast
            home house room office desk table chair door window phone screen keyboard button text word letter message email chat note list task plan project code app build test run check fix update release version design style layout theme color sound haptic
            man woman child friend family team group person world life hand part place case week company system program question number point government night area money story fact month lot study book eye job business issue side kind head far black white red blue green
            able accept access account achieve across act action active actual add address admin admit adopt adult advance advice afford afraid agency agent agree ahead air allow almost alone along already although always amount analysis animal another answer anyone anything anyway anywhere appear apply approach appropriate area argue arm around arrange arrive art article artist assume attack attempt attend attention attitude audience author authority available avoid award aware away
            baby back background balance ball band bank bar base basic basis bear beat beautiful become bed before begin behavior behind believe benefit best better beyond bill billion bit black blood blue board boat body book born both bottom box boy break breakfast breath bridge brief bright bring broad brother brown budget build building bullet bunch burn bus business busy buy
            camera camp campaign cancer candidate capital car card care career careful carry case catch category cause cell center central century certain certainly chain chair challenge chance change channel chapter character charge chart check chemical chest choice choose church circle citizen city civil claim class classic clean clear clearly client climate clock close closely closer clothes cloud club coach coast code coffee cold collect collection college color column combine come comfort command comment commercial commission commit common community company compare compete competition complain complete completely computer concentrate concept concern condition conference confidence confirm conflict congress connect consider consist constant construct contact contain content contest context continue contract contrast control convert cook cool copy core corner corporate correct cost cottage cotton could council count country county couple course court cover create creative credit crew crime crisis critic critical crop cross crowd crown crucial culture cup current customer cycle
            daily damage dance danger dark data database date daughter day dead deal dealer dear death debate decade decide decision deep deeply defeat defend defense deficit define definitely degree delay delete delicate deliver delivery demand demonstrate deny department depend dependent depth derive describe description desert design designer desire desk despite destroy detail detect determine develop development device devote dialogue diamond die diet differ difference different difficult difficulty dig digital dimension dining dinner direct direction directly director dirt disagree disappear disaster discipline discover discuss discussion disease dish disk dismiss display distance distinct distribute distribution district divide division doctor document dog dollar domain domestic dominant dominate door double doubt down downtown dozen draft drag drama draw drawer dream dress drink drive driver drop drug dry due during dust duty
            each eager ear early earn earth ease easily east eastern easy eat economic economy edge edit edition editor educate education effect effective effectively efficiency efficient effort egg eight either elaborate elbow elder elect election electric electronic element eliminate else elsewhere email embrace emerge emergency emotion emphasis emphasize employ employee employer employment empty enable encounter encourage end enemy energy enforce engage engine engineer enhance enjoy enormous enough ensure enter enterprise entertain enthusiasm entire entirely entrance entry environment equal equally equation equip equipment equivalent era error escape especially essay essential establish estate estimate etc ethical ethnic evaluate even evening event eventually ever every everybody everyday everyone everything everywhere evidence evil exact exactly exam examine example exceed excellent except exception exchange excite excited exciting executive exercise exhibit exist existence existing expand expect expectation expense expensive experience experiment expert explain explanation explode explore explosion export expose express expression extend extension extensive extent external extra extraordinary extreme extremely
            fabric face facility fact factor factory fade fail failure fair fairly faith fall false familiar family famous fan fancy fantastic far farm farmer fashion fast fat fate father fault favor favorite fear feature federal fee feed feel feeling fellow female fence festival few fiber fiction field fifteen fifth fifty fight figure file fill film final finally finance financial find finding fine finger finish fire firm first fish fit five fix flag flame flat flavor flee flesh flight float floor flow flower fly focus fold folk follow following food foot football for force foreign forest forever forget forgive form formal format former formula forth fortune forward found foundation founder four fourth frame framework free freedom freeze french frequency frequent frequently fresh friend friendly friendship from front fruit frustrate fuel full fully fun function fund fundamental funny furniture furthermore future
            gain galaxy gallery game gang gap garage garden garlic gas gate gather gaze gear gender gene general generally generate generation generous genius gentle gentleman genuine gesture get ghost giant gift gifted girl give given glad glance glass global glove go goal god gold golden golf good goodbye goods govern government governor grab grace grade gradually graduate grain grand grandfather grandmother grant grass grateful grave great greater greatest greatly green greet grocery ground group grow growth guarantee guard guess guest guide guideline guilt guilty guitar gun guy
            habit hair half hall hand handle hang happen happily happy harbor hard hardly hardware harm harmony hat hate have he head headline headquarters health healthy hear hearing heart heat heaven heavily heavy heel height helicopter hell hello help helpful hence her herb here heritage hero herself hey hi hide high highlight highly highway hill him himself hint hip hire his historian historic historical history hit hold hole holiday hollow holy home homeless honest honey honor hook hope horizon horror horse hospital host hot hotel hour house household housing how however huge human humor hundred hung hungry hunt hunter hurry hurt husband hypothesis
            ice icon idea ideal identify identity ie if ignore ill illegal illness illustrate image imagination imagine immediate immediately impact implement implication imply importance important impose impossible impress impression improve improvement in incentive incident include including income incorporate increase indeed independent index indicate individual industrial industry infant infection inflation influence inform information ingredient initial initially initiative injure injury inner innocent input inquiry inside insight insist inspire install instance instant instead instinct institution instruct instruction instrument insurance intellectual intelligence intend intense intensity intention interact interaction interest interesting internal international internet interpret interpretation interval intervention interview into introduce introduction invade invent invest investigate investigation investment investor invite involve involved involvement iron island issue it item its itself
            jacket jail jam jar jaw jazz jeans jet job join joint joke journal journalist journey joy judge judgment juice jump junior jury just justice justify
            keep key keyboard kick kid kill killer kind king kingdom kiss kitchen knee knife knock know knowledge known
            lab label labor laboratory lack ladder lady lake lamp land landscape language lap large largely laser last late later latest latter laugh launch law lawn lawsuit lawyer lay layer lead leader leadership leading leaf league lean learn learning least leather leave lecture left leg legacy legal legend legislation legitimate lemon length less lesson let letter level liberal library license lid lie life lifestyle lift light like likely limit limitation limited line link lion lip liquid list listen literally literary literature little live living load loan local locate location lock log logic logical lonely long look loop loose lose loss lost lot loud love lovely lover low lower luck lucky lunch lung luxury
            machine mad magazine magic mail main mainly maintain maintenance major majority make maker makeup male mall mama man manage management manager manner manufacturer manufacturing many map march margin mark market marketing marriage married marry mask mass massive master match mate material math matter maximum may maybe mayor me meal mean meaning meanwhile measure measurement meat mechanism media medical medicine medium meet meeting melt member membership memory mental mention menu mere merely mess message metal meter method mexican middle might military milk million mind mine mineral minimal minimum minister minor minority minute miracle mirror miss missile mission mistake mix mixed mixture mobile mode model moderate modern modest modify module mom moment money monitor monkey month mood moon moral more moreover morning mortgage most mostly mother motion motivate motor mount mountain mouse mouth move movement movie much multiple murder muscle museum music musical musician must mutual my myself mystery
            nail naked name narrative narrow nation national native natural naturally nature near nearby nearly neat necessarily necessary necessity neck need negative negotiate neighbor neighborhood neither nerve nervous net network never nevertheless new newly news newspaper next nice night nine no nobody nod noise none nonetheless noon nor normal normally north northern nose not note nothing notice notion novel now nowhere nuclear number numerous nurse nut
            object objective obligation observe observer obtain obvious obviously occasion occasional occasionally occupy occur ocean odd odds of off offense offensive offer office officer official often oh oil ok okay old olympic on once one ongoing onion online only onto open opening operate operating operation operator opinion opponent opportunity oppose opposite opposition option or orange orbit order ordinary organic organization organize orientation origin original originally other others otherwise ought our ourselves out outcome outdoor outer outlet outline output outside outstanding oven over overall overcome overlook owe own owner
            pace pack package page pain painful paint painter painting pair pale palm pan panel panic pant paper parent park parking part participant participate participation particular particularly partly partner partnership party pass passage passenger passion past patch path patient pattern pause pay payment peace peak peer penalty people pepper per perceive percentage perception perfect perfectly perform performance perhaps period permanent permission permit person personal personality personally perspective persuade pet phase phenomenon philosophy phone photo photograph photographer phrase physical physically physician piano pick picture pie piece pig pile pilot pin pine pink pipe pitch place plain plan plane planet planning plant plastic plate platform play player please pleasure plenty plot plus pocket poem poet poetry point pole police policy political politically politician politics poll pollution pond pool poor pop popular population porch port portion portrait portray pose position positive possess possibility possible possibly post pot potato potential potentially pound pour poverty powder power powerful practical practice pray prayer precisely predict prefer preference pregnant premise preparation prepare prescription presence present presentation preserve president press pressure pretend pretty prevent previous previously price pride priest primarily primary prime principal principle print prior priority prison prisoner privacy private probably problem procedure proceed process produce producer product production profession professional professor profile profit program progress project prominent promise promote prompt proof proper properly property proportion proposal propose proposed prosecutor prospect protect protection protein protest proud prove provide provider province provision psychological psychologist psychology public publication publicly publish publisher pull pulse pump punch punish purchase pure purpose pursue push put
            qualify quality quantity quarter quarterback question quick quickly quiet quietly quit quite quote
            race racial radical radio rail rain raise range rank rapid rapidly rare rarely rate rather rating ratio raw reach react reaction read reader reading ready real reality realize really reason reasonable recall receive recent recently recipe recognition recognize recommend recommendation record recording recover recovery recruit red reduce reduction refer reference reflect reflection reform refrigerator refuse regard regarding regardless regime region regional register regular regularly regulate regulation reinforce reject relate relation relationship relative relatively relax release relevant relief religion religious rely remain remaining remember remind remote remove repeat replace reply report reporter represent representation representative republican reputation request require requirement research researcher resemble reservation resident resist resistance resolution resolve resort resource respect respond response responsibility responsible rest restaurant restore restrict restriction result retain retire retirement return reveal revenue review revolution reward rhythm rice rich rid ride rider ridge rifle right ring rise risk river road rock role roll romantic roof room root rope rose rough roughly round route routine row royal rub rubber ruin rule run running rural rush russian
            sacred sad safe safety sake salad salary sale sales salt same sample sanction sand sandwich satellite satisfaction satisfy sauce save saving say scale scan scandal scare scared scary scenario scene schedule scheme scholar scholarship school science scientific scientist scope score scream screen script sea seal search season seat second secret secretary section sector secure security see seed seek seem segment seize select selection self sell senate senator send senior sense sensitive sentence separate sequence series serious seriously servant serve service session set setting settle settlement seven several severe sex sexual shade shadow shake shall shallow shape share sharp she sheet shelf shell shelter shift shine ship shirt shit shock shoe shoot shooting shop shopping shore short shortly shot should shoulder shout show shower shrug shut sick side sigh sight sign signal signature significance significant significantly silence silent silver similar similarly simple simply sin since sing singer single sink sir sister sit site situation six size ski skill skin sky slave sleep slice slide slight slightly slip slow slowly small smart smell smile smoke smooth snap snow so soap soccer social society sock soft software soil solar soldier solid solution solve some somebody somehow someone something sometimes somewhat somewhere son song soon sophisticated sorry sort soul sound soup source south southern space spanish speak speaker special specialist species specific specifically speech speed spend spending spin spirit spiritual split spoke spokesman sport spot spread spring square squeeze stability stable stack staff stage stair stake stand standard standing star stare start state statement station statistics status stay steady steal steam steel steep steer stem step stick stiff still stimulus stock stomach stone stop storage store storm story straight strange stranger strategic strategy stream street strength strengthen stress stretch strike string strip stroke strong strongly structure struggle student studio study stuff stumble stupid style subject submit subsequent substance substantial succeed success successful successfully such sudden suddenly suffer sufficient sugar suggest suggestion suicide suit suitable suite summer summit sun super supply support supporter suppose supposed sure surely surface surgery surprise surprised surprising surprisingly surround survey survival survive survivor suspect sustain swear sweep sweet swim swing switch symbol symptom system
            table tablespoon tactic tail take tale talent talk tall tank tap tape target task taste tax taxpayer tea teach teacher teaching team tear teaspoon technical technique technology teen teenager telephone telescope television tell temperature temporary ten tend tendency tennis tent term terms terrible territory terror terrorism test testify testimony testing text texture than thank thanks that the theater their them theme themselves then theory therapy there therefore these they thick thin thing think thinking third thirty this those though thought thousand threat threaten three throat through throughout throw thus ticket tie tight time tiny tip tire tired tissue title to tobacco today toe together tomato tomorrow tone tongue tonight too tool tooth top topic toss total totally touch tough tour tournament toward towards tower town toy trace track trade tradition traditional traffic tragedy trail train training transfer transform transformation transition translate translation transportation trap trash travel treat treatment treaty tree tremendous trend trial tribe trick trip troop trouble truck true truly trust truth try tube tunnel turn tv twelve twenty twice twin two type typical typically
            ugly ultimate ultimately unable uncle under undergo understand understanding unfortunately uniform union unique unit united universal universe university unknown unless unlike unlikely until unusual up upon upper urban urge us use used useful user usual usually utility
            vacation vaccine vacuum vague valid valley valuable value van vanish variable variation variety various vary vast vegetable vehicle vendor venture verify version versus very vessel veteran via victim victory video view viewer village violate violation violence violent virtual virtually virtue virus visible vision visit visitor visual vital voice volume volunteer vote voter vs vulnerable
            wage wait wake walk wall wander want war warm warn warning wash waste watch water wave way we weak wealth wealthy weapon wear weather wedding week weekend weekly weigh weight welcome welfare well west western wet whale what whatever wheel when whenever where whereas whether which while whisper white who whoever whole whom whose why wide widely widespread widow wife wild will willing win wind window wine wing winner winter wipe wire wisdom wise wish with withdraw within without witness woman wonder wonderful wood wooden word work worker working works workshop world worried worry worth would wound wrap write writer writing written wrong
            yard yeah year yell yellow yes yesterday yet yield you young your yours yourself youth
            zero zone zoom
        """.trimIndent()
        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()
        tokens.mapIndexed { idx, word ->
            val score = (2000 - idx).coerceAtLeast(100)
            word to score
        }
    }

    val commonTypos: Map<String, String> = mapOf(
        "teh" to "the",
        "hte" to "the",
        "taht" to "that",
        "tath" to "that",
        "adn" to "and",
        "nad" to "and",
        "waht" to "what",
        "hwat" to "what",
        "wiht" to "with",
        "wtih" to "with",
        "fro" to "for",
        "fo" to "of",
        "ot" to "to",
        "si" to "is",
        "im" to "I'm",
        "ive" to "I've",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "wouldnt" to "wouldn't",
        "shouldnt" to "shouldn't",
        "couldnt" to "couldn't",
        "youre" to "you're",
        "theyre" to "they're",
        "thats" to "that's",
        "whats" to "what's",
        "heres" to "here's",
        "theres" to "there's",
        "lets" to "let's",
        "recieve" to "receive",
        "seperate" to "separate",
        "definately" to "definitely",
        "occured" to "occurred",
        "untill" to "until",
        "wich" to "which",
        "becuase" to "because",
        "beacuse" to "because",
        "thier" to "their",
        "freind" to "friend",
        "tommorrow" to "tomorrow",
        "tommorow" to "tomorrow",
        "adress" to "address",
        "beleive" to "believe",
        "acheive" to "achieve",
        "calender" to "calendar",
        "comming" to "coming",
        "enviroment" to "environment",
        "goverment" to "government",
        "independant" to "independent",
        "knowlege" to "knowledge",
        "neccessary" to "necessary",
        "occassion" to "occasion",
        "persue" to "pursue",
        "publically" to "publicly",
        "realy" to "really",
        "refered" to "referred",
        "religous" to "religious",
        "rythm" to "rhythm",
        "succesful" to "successful",
        "suprise" to "surprise",
        "truely" to "truly",
        "writting" to "writing",
        "i" to "I"
    )

    val bigrams: Map<String, List<Pair<String, Int>>> = mapOf(
        "i" to listOf("am" to 95, "have" to 90, "will" to 88, "think" to 84, "just" to 80, "need" to 76),
        "i'm" to listOf("on" to 92, "not" to 90, "good" to 85, "ready" to 82, "here" to 78),
        "you" to listOf("are" to 95, "can" to 90, "have" to 86, "know" to 82, "want" to 78),
        "thank" to listOf("you" to 98, "goodness" to 65, "everyone" to 60),
        "thanks" to listOf("for" to 95, "so" to 88, "again" to 82, "a" to 76),
        "looking" to listOf("forward" to 96, "for" to 88, "at" to 82, "good" to 74),
        "forward" to listOf("to" to 98),
        "on" to listOf("my" to 90, "the" to 88, "time" to 82, "it" to 78),
        "my" to listOf("way" to 92, "phone" to 84, "mind" to 80, "best" to 76),
        "see" to listOf("you" to 96, "what" to 84, "if" to 80, "that" to 74),
        "how" to listOf("are" to 95, "is" to 88, "about" to 84, "much" to 78, "long" to 74),
        "are" to listOf("you" to 96, "we" to 80, "they" to 76),
        "let" to listOf("me" to 95, "us" to 85, "it" to 80, "them" to 74),
        "me" to listOf("know" to 96, "see" to 84, "check" to 80, "a" to 74),
        "let's" to listOf("go" to 92, "do" to 88, "meet" to 84, "check" to 78),
        "good" to listOf("morning" to 92, "luck" to 86, "night" to 84, "idea" to 80, "job" to 78),
        "have" to listOf("a" to 94, "to" to 88, "you" to 84, "been" to 80),
        "a" to listOf("good" to 90, "great" to 88, "quick" to 82, "lot" to 80, "new" to 76),
        "want" to listOf("to" to 96, "some" to 76, "a" to 72),
        "need" to listOf("to" to 96, "a" to 84, "some" to 78, "help" to 74),
        "going" to listOf("to" to 97, "back" to 82, "home" to 78, "out" to 74),
        "in" to listOf("the" to 94, "a" to 86, "touch" to 82, "time" to 78),
        "for" to listOf("the" to 92, "a" to 86, "your" to 84, "sure" to 80),
        "with" to listOf("you" to 92, "the" to 88, "a" to 82, "me" to 78),
        "this" to listOf("is" to 95, "week" to 84, "morning" to 80, "one" to 76),
        "that" to listOf("is" to 94, "was" to 88, "sounds" to 84, "works" to 80),
        "sounds" to listOf("good" to 95, "great" to 90, "like" to 84),
        "happy" to listOf("to" to 92, "birthday" to 86, "with" to 78),
        "sorry" to listOf("for" to 92, "about" to 88, "to" to 84),
        "please" to listOf("let" to 90, "send" to 84, "check" to 80, "note" to 76),
        "can" to listOf("you" to 95, "we" to 86, "i" to 84, "help" to 76),
        "could" to listOf("you" to 95, "we" to 84, "be" to 78),
        "would" to listOf("you" to 92, "be" to 88, "love" to 84, "like" to 82),
        "no" to listOf("problem" to 95, "worries" to 88, "way" to 80),
        "at" to listOf("the" to 92, "least" to 84, "home" to 80, "work" to 76),
        "to" to listOf("the" to 90, "be" to 88, "do" to 84, "see" to 80, "get" to 78),
        "of" to listOf("the" to 94, "course" to 88, "this" to 80, "all" to 76),
        "it" to listOf("is" to 94, "was" to 88, "will" to 82, "looks" to 78),
        "right" to listOf("now" to 96, "away" to 88, "here" to 82),
        "as" to listOf("soon" to 90, "well" to 86, "always" to 78),
        "soon" to listOf("as" to 96, "enough" to 76)
    )

    val trigrams: Map<String, List<Pair<String, Int>>> = mapOf(
        "on my" to listOf("way" to 99, "phone" to 84, "own" to 78),
        "thank you" to listOf("so" to 94, "for" to 90, "very" to 82),
        "you so" to listOf("much" to 99),
        "looking forward" to listOf("to" to 99),
        "forward to" to listOf("it" to 94, "seeing" to 88, "hearing" to 84),
        "let me" to listOf("know" to 99, "check" to 86, "see" to 82),
        "how are" to listOf("you" to 99, "things" to 82),
        "see you" to listOf("soon" to 95, "later" to 90, "tomorrow" to 86),
        "as soon" to listOf("as" to 99),
        "soon as" to listOf("possible" to 96, "i" to 84, "we" to 80),
        "sounds good" to listOf("to" to 94),
        "good to" to listOf("me" to 96, "go" to 86, "see" to 80),
        "have a" to listOf("great" to 94, "good" to 90, "nice" to 84),
        "out of" to listOf("the" to 92, "office" to 86, "town" to 80),
        "talk to" to listOf("you" to 98),
        "to you" to listOf("soon" to 92, "later" to 88)
    )

    val emojiCatalog: List<EmojiEntry> = listOf(
        EmojiEntry("😀", EmojiCategory.SMILEYS, listOf("smile", "happy", "grin", "joy")),
        EmojiEntry("😄", EmojiCategory.SMILEYS, listOf("happy", "laugh", "smile", "cheerful")),
        EmojiEntry("😂", EmojiCategory.SMILEYS, listOf("laugh", "lol", "funny", "tears", "joy")),
        EmojiEntry("🤣", EmojiCategory.SMILEYS, listOf("rofl", "laugh", "funny", "hilarious")),
        EmojiEntry("😊", EmojiCategory.SMILEYS, listOf("blush", "warm", "smile", "thanks")),
        EmojiEntry("😉", EmojiCategory.SMILEYS, listOf("wink", "flirt", "playful")),
        EmojiEntry("😍", EmojiCategory.SMILEYS, listOf("love", "heart", "eyes", "adore")),
        EmojiEntry("🤩", EmojiCategory.SMILEYS, listOf("star", "excited", "wow", "amazing")),
        EmojiEntry("🤔", EmojiCategory.SMILEYS, listOf("think", "hmm", "wonder", "question")),
        EmojiEntry("🤨", EmojiCategory.SMILEYS, listOf("skeptic", "hmm", "sus")),
        EmojiEntry("😎", EmojiCategory.SMILEYS, listOf("cool", "sunglasses", "awesome")),
        EmojiEntry("🥳", EmojiCategory.SMILEYS, listOf("party", "celebrate", "birthday")),
        EmojiEntry("😅", EmojiCategory.SMILEYS, listOf("sweat", "awkward", "phew")),
        EmojiEntry("😌", EmojiCategory.SMILEYS, listOf("relief", "calm", "peace")),
        EmojiEntry("😴", EmojiCategory.SMILEYS, listOf("sleep", "tired", "zzz")),
        EmojiEntry("😢", EmojiCategory.SMILEYS, listOf("sad", "cry", "tear")),
        EmojiEntry("😭", EmojiCategory.SMILEYS, listOf("sob", "cry", "sad")),
        EmojiEntry("😤", EmojiCategory.SMILEYS, listOf("focus", "frustrated", "huff")),
        EmojiEntry("🤯", EmojiCategory.SMILEYS, listOf("mindblown", "wow", "shock")),
        EmojiEntry("🫡", EmojiCategory.SMILEYS, listOf("salute", "respect", "yes", "roger")),
        EmojiEntry("🫠", EmojiCategory.SMILEYS, listOf("melt", "oops", "heat")),
        EmojiEntry("👀", EmojiCategory.SMILEYS, listOf("eyes", "look", "watch", "see")),

        EmojiEntry("👍", EmojiCategory.GESTURES, listOf("thumbs", "up", "yes", "ok", "good", "approve")),
        EmojiEntry("👎", EmojiCategory.GESTURES, listOf("thumbs", "down", "no", "bad")),
        EmojiEntry("👏", EmojiCategory.GESTURES, listOf("clap", "applause", "bravo", "congrats")),
        EmojiEntry("🙌", EmojiCategory.GESTURES, listOf("hands", "celebrate", "praise", "yay")),
        EmojiEntry("🙏", EmojiCategory.GESTURES, listOf("please", "thanks", "pray", "grateful")),
        EmojiEntry("🤝", EmojiCategory.GESTURES, listOf("handshake", "deal", "agree")),
        EmojiEntry("👋", EmojiCategory.GESTURES, listOf("wave", "hello", "hi", "bye")),
        EmojiEntry("✌️", EmojiCategory.GESTURES, listOf("peace", "victory", "two")),
        EmojiEntry("🤞", EmojiCategory.GESTURES, listOf("luck", "hope", "crossed")),
        EmojiEntry("👌", EmojiCategory.GESTURES, listOf("ok", "perfect", "nice")),
        EmojiEntry("🫶", EmojiCategory.GESTURES, listOf("heart", "love", "hands")),
        EmojiEntry("💪", EmojiCategory.GESTURES, listOf("strong", "muscle", "gym", "power")),
        EmojiEntry("✍️", EmojiCategory.GESTURES, listOf("write", "note", "sign")),
        EmojiEntry("👆", EmojiCategory.GESTURES, listOf("up", "point", "above")),

        EmojiEntry("💻", EmojiCategory.WORK_TECH, listOf("laptop", "code", "work", "computer", "dev")),
        EmojiEntry("⌨️", EmojiCategory.WORK_TECH, listOf("keyboard", "type", "keys")),
        EmojiEntry("📱", EmojiCategory.WORK_TECH, listOf("phone", "mobile", "app")),
        EmojiEntry("🚀", EmojiCategory.WORK_TECH, listOf("rocket", "launch", "ship", "fast")),
        EmojiEntry("⚙️", EmojiCategory.WORK_TECH, listOf("gear", "settings", "config", "engine")),
        EmojiEntry("🔧", EmojiCategory.WORK_TECH, listOf("wrench", "fix", "tool", "build")),
        EmojiEntry("🧪", EmojiCategory.WORK_TECH, listOf("test", "lab", "experiment")),
        EmojiEntry("🐛", EmojiCategory.WORK_TECH, listOf("bug", "debug", "issue")),
        EmojiEntry("📌", EmojiCategory.WORK_TECH, listOf("pin", "note", "important")),
        EmojiEntry("📝", EmojiCategory.WORK_TECH, listOf("memo", "note", "write", "draft")),
        EmojiEntry("📅", EmojiCategory.WORK_TECH, listOf("calendar", "date", "schedule")),
        EmojiEntry("⏰", EmojiCategory.WORK_TECH, listOf("clock", "time", "alarm", "deadline")),
        EmojiEntry("🔒", EmojiCategory.WORK_TECH, listOf("lock", "private", "secure", "offline")),
        EmojiEntry("💡", EmojiCategory.WORK_TECH, listOf("idea", "bulb", "smart", "tip")),
        EmojiEntry("📊", EmojiCategory.WORK_TECH, listOf("chart", "data", "stats")),

        EmojiEntry("☕", EmojiCategory.NATURE_FOOD, listOf("coffee", "cafe", "tea", "morning")),
        EmojiEntry("🍕", EmojiCategory.NATURE_FOOD, listOf("pizza", "food", "lunch", "dinner")),
        EmojiEntry("🍔", EmojiCategory.NATURE_FOOD, listOf("burger", "food", "lunch")),
        EmojiEntry("🌮", EmojiCategory.NATURE_FOOD, listOf("taco", "food")),
        EmojiEntry("🍣", EmojiCategory.NATURE_FOOD, listOf("sushi", "food")),
        EmojiEntry("🍎", EmojiCategory.NATURE_FOOD, listOf("apple", "fruit", "healthy")),
        EmojiEntry("🌞", EmojiCategory.NATURE_FOOD, listOf("sun", "morning", "bright")),
        EmojiEntry("🌙", EmojiCategory.NATURE_FOOD, listOf("moon", "night", "sleep")),
        EmojiEntry("🌧️", EmojiCategory.NATURE_FOOD, listOf("rain", "weather")),
        EmojiEntry("🌿", EmojiCategory.NATURE_FOOD, listOf("leaf", "plant", "nature", "green")),
        EmojiEntry("🌊", EmojiCategory.NATURE_FOOD, listOf("wave", "ocean", "sea")),
        EmojiEntry("🐶", EmojiCategory.NATURE_FOOD, listOf("dog", "puppy", "pet")),
        EmojiEntry("🐱", EmojiCategory.NATURE_FOOD, listOf("cat", "kitty", "pet")),

        EmojiEntry("✨", EmojiCategory.SYMBOLS, listOf("sparkle", "magic", "shine", "new", "clean")),
        EmojiEntry("🔥", EmojiCategory.SYMBOLS, listOf("fire", "hot", "lit", "awesome")),
        EmojiEntry("❤️", EmojiCategory.SYMBOLS, listOf("heart", "love", "red")),
        EmojiEntry("💙", EmojiCategory.SYMBOLS, listOf("blue", "heart", "love")),
        EmojiEntry("🧡", EmojiCategory.SYMBOLS, listOf("orange", "heart")),
        EmojiEntry("✅", EmojiCategory.SYMBOLS, listOf("check", "done", "yes", "pass", "ok")),
        EmojiEntry("❌", EmojiCategory.SYMBOLS, listOf("cross", "no", "fail", "wrong", "delete")),
        EmojiEntry("⚠️", EmojiCategory.SYMBOLS, listOf("warn", "alert", "caution")),
        EmojiEntry("💯", EmojiCategory.SYMBOLS, listOf("100", "perfect", "score")),
        EmojiEntry("🎉", EmojiCategory.SYMBOLS, listOf("party", "tada", "celebrate", "congrats")),
        EmojiEntry("⭐", EmojiCategory.SYMBOLS, listOf("star", "favorite", "best")),
        EmojiEntry("⚡", EmojiCategory.SYMBOLS, listOf("bolt", "fast", "speed", "lightning")),
        EmojiEntry("🎯", EmojiCategory.SYMBOLS, listOf("target", "goal", "focus", "accurate")),
        EmojiEntry("➡️", EmojiCategory.SYMBOLS, listOf("arrow", "right", "next")),
        EmojiEntry("•", EmojiCategory.SYMBOLS, listOf("bullet", "dot"))
    )
}
