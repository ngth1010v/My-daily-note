package io.github.ngth1010v.mydailynote.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrunchDining
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.Kayaking
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Moped
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NoDrinks
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TempleBuddhist
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon keys stored with entries, mapped to Material icons and shown by group in the picker
 * (groups modeled on Outgo's category icon picker). Keys are persisted: never rename or remove one.
 */
object EntryIcons {
    class Group(@StringRes val title: Int, val icons: List<Pair<String, ImageVector>>)

    val groups: List<Group> = listOf(
        Group(R.string.icon_group_food, listOf(
            "food" to Icons.Filled.Restaurant,
            "coffee" to Icons.Filled.Coffee,
            "water" to Icons.Filled.LocalDrink,
            "tea" to Icons.Filled.EmojiFoodBeverage,
            "breakfast" to Icons.Filled.FreeBreakfast,
            "brunch" to Icons.Filled.BrunchDining,
            "lunch" to Icons.Filled.LunchDining,
            "dinner" to Icons.Filled.DinnerDining,
            "noodles" to Icons.Filled.RamenDining,
            "rice" to Icons.Filled.RiceBowl,
            "meal" to Icons.Filled.SetMeal,
            "soup" to Icons.Filled.SoupKitchen,
            "bakery" to Icons.Filled.BakeryDining,
            "pizza" to Icons.Filled.LocalPizza,
            "fast_food" to Icons.Filled.Fastfood,
            "kebab" to Icons.Filled.KebabDining,
            "takeout" to Icons.Filled.TakeoutDining,
            "egg" to Icons.Filled.Egg,
            "cake" to Icons.Filled.Cake,
            "cookie" to Icons.Filled.Cookie,
            "ice_cream" to Icons.Filled.Icecream,
            "wine" to Icons.Filled.WineBar,
            "beer" to Icons.Filled.SportsBar,
            "cocktail" to Icons.Filled.LocalBar,
            "liquor" to Icons.Filled.Liquor,
            "cook" to Icons.Filled.OutdoorGrill,
            "no_food" to Icons.Filled.NoFood,
            "no_drinks" to Icons.Filled.NoDrinks,
        )),
        Group(R.string.icon_group_health, listOf(
            "pill" to Icons.Filled.Medication,
            "heart" to Icons.Filled.Favorite,
            "heart_rate" to Icons.Filled.MonitorHeart,
            "medical" to Icons.Filled.MedicalServices,
            "hospital" to Icons.Filled.LocalHospital,
            "vaccine" to Icons.Filled.Vaccines,
            "bandage" to Icons.Filled.Healing,
            "blood" to Icons.Filled.Bloodtype,
            "weight" to Icons.Filled.MonitorWeight,
            "meditate" to Icons.Filled.SelfImprovement,
            "mind" to Icons.Filled.Psychology,
            "spa" to Icons.Filled.Spa,
            "sleep" to Icons.Filled.Bedtime,
            "wake" to Icons.Filled.WbSunny,
            "alarm" to Icons.Filled.Alarm,
            "shower" to Icons.Filled.Shower,
            "bath" to Icons.Filled.Bathtub,
            "wash_hands" to Icons.Filled.CleanHands,
            "skincare" to Icons.Filled.Face,
            "haircut" to Icons.Filled.ContentCut,
            "mask" to Icons.Filled.Masks,
            "eye" to Icons.Filled.Visibility,
            "drop" to Icons.Filled.WaterDrop,
            "no_smoking" to Icons.Filled.SmokeFree,
            "smoking" to Icons.Filled.SmokingRooms,
        )),
        Group(R.string.icon_group_sport, listOf(
            "fitness" to Icons.Filled.FitnessCenter,
            "run" to Icons.AutoMirrored.Filled.DirectionsRun,
            "walk" to Icons.AutoMirrored.Filled.DirectionsWalk,
            "bike" to Icons.AutoMirrored.Filled.DirectionsBike,
            "swim" to Icons.Filled.Pool,
            "hike" to Icons.Filled.Hiking,
            "yoga" to Icons.Filled.SportsGymnastics,
            "martial_arts" to Icons.Filled.SportsMartialArts,
            "soccer" to Icons.Filled.SportsSoccer,
            "basketball" to Icons.Filled.SportsBasketball,
            "tennis" to Icons.Filled.SportsTennis,
            "volleyball" to Icons.Filled.SportsVolleyball,
            "baseball" to Icons.Filled.SportsBaseball,
            "golf" to Icons.Filled.SportsGolf,
            "cricket" to Icons.Filled.SportsCricket,
            "rowing" to Icons.Filled.Rowing,
            "kayak" to Icons.Filled.Kayaking,
            "surf" to Icons.Filled.Surfing,
            "skate" to Icons.Filled.Skateboarding,
            "ski" to Icons.Filled.DownhillSkiing,
            "snowboard" to Icons.Filled.Snowboarding,
            "sport" to Icons.Filled.Sports,
            "trophy" to Icons.Filled.EmojiEvents,
            "timer" to Icons.Filled.Timer,
        )),
        Group(R.string.icon_group_work, listOf(
            "work" to Icons.Filled.Work,
            "code" to Icons.Filled.Code,
            "terminal" to Icons.Filled.Terminal,
            "bug" to Icons.Filled.BugReport,
            "laptop" to Icons.Filled.Laptop,
            "computer" to Icons.Filled.Computer,
            "study" to Icons.Filled.School,
            "book" to Icons.AutoMirrored.Filled.MenuBook,
            "reading" to Icons.Filled.AutoStories,
            "write" to Icons.Filled.EditNote,
            "article" to Icons.AutoMirrored.Filled.Article,
            "newspaper" to Icons.Filled.Newspaper,
            "idea" to Icons.Filled.Lightbulb,
            "science" to Icons.Filled.Science,
            "calculate" to Icons.Filled.Calculate,
            "translate" to Icons.Filled.Translate,
            "quiz" to Icons.Filled.Quiz,
            "task" to Icons.Filled.TaskAlt,
            "meeting" to Icons.Filled.CoPresent,
            "event" to Icons.Filled.Event,
            "calendar" to Icons.Filled.CalendarMonth,
            "schedule" to Icons.Filled.Schedule,
            "print" to Icons.Filled.Print,
            "draw" to Icons.Filled.Draw,
        )),
        Group(R.string.icon_group_home, listOf(
            "home" to Icons.Filled.Home,
            "clean" to Icons.Filled.CleaningServices,
            "laundry" to Icons.Filled.LocalLaundryService,
            "iron" to Icons.Filled.Iron,
            "dishes" to Icons.Filled.Countertops,
            "fridge" to Icons.Filled.Kitchen,
            "microwave" to Icons.Filled.Microwave,
            "bed" to Icons.Filled.Bed,
            "sofa" to Icons.Filled.Weekend,
            "chair" to Icons.Filled.Chair,
            "plant" to Icons.Filled.LocalFlorist,
            "garden" to Icons.Filled.Yard,
            "grass" to Icons.Filled.Grass,
            "repair" to Icons.Filled.Handyman,
            "tools" to Icons.Filled.Build,
            "plumbing" to Icons.Filled.Plumbing,
            "electric" to Icons.Filled.ElectricalServices,
            "trash" to Icons.Filled.DeleteSweep,
            "recycle" to Icons.Filled.Recycling,
            "key" to Icons.Filled.Key,
            "lock" to Icons.Filled.Lock,
            "garage" to Icons.Filled.Garage,
            "light" to Icons.Filled.EmojiObjects,
            "heater" to Icons.Filled.LocalFireDepartment,
        )),
        Group(R.string.icon_group_transport, listOf(
            "car" to Icons.Filled.DirectionsCar,
            "bus" to Icons.Filled.DirectionsBus,
            "train" to Icons.Filled.Train,
            "subway" to Icons.Filled.Subway,
            "tram" to Icons.Filled.Tram,
            "motorbike" to Icons.Filled.TwoWheeler,
            "moped" to Icons.Filled.Moped,
            "e_scooter" to Icons.Filled.ElectricScooter,
            "e_bike" to Icons.Filled.ElectricBike,
            "taxi" to Icons.Filled.LocalTaxi,
            "fuel" to Icons.Filled.LocalGasStation,
            "charging" to Icons.Filled.EvStation,
            "parking" to Icons.Filled.LocalParking,
            "flight" to Icons.Filled.Flight,
            "boat" to Icons.Filled.DirectionsBoat,
            "delivery" to Icons.Filled.LocalShipping,
            "car_repair" to Icons.Filled.CarRepair,
            "car_wash" to Icons.Filled.LocalCarWash,
            "commute" to Icons.Filled.Commute,
            "traffic" to Icons.Filled.Traffic,
        )),
        Group(R.string.icon_group_shopping, listOf(
            "shopping" to Icons.Filled.ShoppingCart,
            "bag" to Icons.Filled.ShoppingBag,
            "mall" to Icons.Filled.LocalMall,
            "store" to Icons.Filled.Storefront,
            "grocery" to Icons.Filled.LocalGroceryStore,
            "clothes" to Icons.Filled.Checkroom,
            "jewelry" to Icons.Filled.Diamond,
            "gift" to Icons.Filled.CardGiftcard,
            "sale" to Icons.Filled.Sell,
            "receipt" to Icons.Filled.Receipt,
            "money" to Icons.Filled.Payments,
            "card" to Icons.Filled.CreditCard,
            "bank" to Icons.Filled.AccountBalance,
            "wallet" to Icons.Filled.AccountBalanceWallet,
            "savings" to Icons.Filled.Savings,
            "dollar" to Icons.Filled.AttachMoney,
            "exchange" to Icons.Filled.CurrencyExchange,
            "invest" to Icons.AutoMirrored.Filled.TrendingUp,
            "paid" to Icons.Filled.Paid,
            "bill" to Icons.Filled.RequestQuote,
        )),
        Group(R.string.icon_group_fun, listOf(
            "movie" to Icons.Filled.Movie,
            "theater" to Icons.Filled.TheaterComedy,
            "tv" to Icons.Filled.Tv,
            "music" to Icons.Filled.MusicNote,
            "headphones" to Icons.Filled.Headphones,
            "podcast" to Icons.Filled.Podcasts,
            "radio" to Icons.Filled.Radio,
            "piano" to Icons.Filled.Piano,
            "game" to Icons.Filled.SportsEsports,
            "videogame" to Icons.Filled.VideogameAsset,
            "puzzle" to Icons.Filled.Extension,
            "toys" to Icons.Filled.Toys,
            "casino" to Icons.Filled.Casino,
            "photo" to Icons.Filled.PhotoCamera,
            "paint" to Icons.Filled.Palette,
            "brush" to Icons.Filled.Brush,
            "ticket" to Icons.Filled.LocalActivity,
            "party" to Icons.Filled.Celebration,
            "nightlife" to Icons.Filled.Nightlife,
            "library" to Icons.Filled.LocalLibrary,
        )),
        Group(R.string.icon_group_social, listOf(
            "call" to Icons.Filled.Call,
            "video_call" to Icons.Filled.VideoCall,
            "chat" to Icons.AutoMirrored.Filled.Chat,
            "forum" to Icons.Filled.Forum,
            "mail" to Icons.Filled.Email,
            "people" to Icons.Filled.People,
            "group" to Icons.Filled.Groups,
            "family" to Icons.Filled.FamilyRestroom,
            "child" to Icons.Filled.ChildCare,
            "stroller" to Icons.Filled.ChildFriendly,
            "elderly" to Icons.Filled.Elderly,
            "pet" to Icons.Filled.Pets,
            "love" to Icons.Filled.VolunteerActivism,
            "handshake" to Icons.Filled.Handshake,
            "add_friend" to Icons.Filled.PersonAdd,
            "church" to Icons.Filled.Church,
            "mosque" to Icons.Filled.Mosque,
            "temple" to Icons.Filled.TempleBuddhist,
        )),
        Group(R.string.icon_group_travel, listOf(
            "luggage" to Icons.Filled.Luggage,
            "beach" to Icons.Filled.BeachAccess,
            "park" to Icons.Filled.Park,
            "forest" to Icons.Filled.Forest,
            "mountain" to Icons.Filled.Landscape,
            "terrain" to Icons.Filled.Terrain,
            "camping" to Icons.Filled.Cabin,
            "hotel" to Icons.Filled.Hotel,
            "map" to Icons.Filled.Map,
            "explore" to Icons.Filled.Explore,
            "world" to Icons.Filled.Public,
            "attractions" to Icons.Filled.Attractions,
            "sailing" to Icons.Filled.Sailing,
            "sunset" to Icons.Filled.WbTwilight,
            "night" to Icons.Filled.NightsStay,
            "snow" to Icons.Filled.AcUnit,
            "storm" to Icons.Filled.Thunderstorm,
            "umbrella" to Icons.Filled.Umbrella,
            "waves" to Icons.Filled.Water,
            "eco" to Icons.Filled.Eco,
            "farm" to Icons.Filled.Agriculture,
        )),
        Group(R.string.icon_group_other, listOf(
            "check" to Icons.Filled.CheckCircle,
            "star" to Icons.Filled.Star,
            "flag" to Icons.Filled.Flag,
            "bookmark" to Icons.Filled.Bookmark,
            "pin" to Icons.Filled.PushPin,
            "bolt" to Icons.Filled.Bolt,
            "fire" to Icons.Filled.Whatshot,
            "happy" to Icons.Filled.SentimentSatisfied,
            "sad" to Icons.Filled.SentimentDissatisfied,
            "thumb_up" to Icons.Filled.ThumbUp,
            "thumb_down" to Icons.Filled.ThumbDown,
            "warning" to Icons.Filled.Warning,
            "info" to Icons.Filled.Info,
            "hourglass" to Icons.Filled.HourglassEmpty,
            "phone" to Icons.Filled.Smartphone,
            "wifi" to Icons.Filled.Wifi,
            "battery" to Icons.Filled.BatteryFull,
            "cloud" to Icons.Filled.Cloud,
        )),
    )

    val all: List<Pair<String, ImageVector>> = groups.flatMap { it.icons }
    private val byKey = all.toMap()

    fun icon(key: String?): ImageVector? = key?.let(byKey::get)
}
