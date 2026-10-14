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
    /** [meaning]: English + Vietnamese words for what the icon stands for; new actions are matched against it. */
    data class Item(val key: String, val vector: ImageVector, val meaning: String)

    class Group(@StringRes val title: Int, val icons: List<Item>)

    val groups: List<Group> = listOf(
        Group(R.string.icon_group_food, listOf(
            Item("food", Icons.Filled.Restaurant, "eat food meal restaurant, ăn cơm nhà hàng"),
            Item("coffee", Icons.Filled.Coffee, "drink coffee, uống cà phê"),
            Item("water", Icons.Filled.LocalDrink, "drink water, uống nước"),
            Item("tea", Icons.Filled.EmojiFoodBeverage, "drink tea, uống trà"),
            Item("breakfast", Icons.Filled.FreeBreakfast, "eat breakfast morning meal, ăn sáng"),
            Item("brunch", Icons.Filled.BrunchDining, "brunch, bữa ăn nửa buổi"),
            Item("lunch", Icons.Filled.LunchDining, "eat lunch burger, ăn trưa"),
            Item("dinner", Icons.Filled.DinnerDining, "eat dinner evening meal, ăn tối"),
            Item("noodles", Icons.Filled.RamenDining, "noodles ramen pho, mì phở bún"),
            Item("rice", Icons.Filled.RiceBowl, "rice bowl, cơm bát cơm"),
            Item("meal", Icons.Filled.SetMeal, "set meal fish dish, mâm cơm món cá"),
            Item("soup", Icons.Filled.SoupKitchen, "soup kitchen cook soup, nấu canh súp"),
            Item("bakery", Icons.Filled.BakeryDining, "bread bakery croissant, bánh mì tiệm bánh"),
            Item("pizza", Icons.Filled.LocalPizza, "pizza"),
            Item("fast_food", Icons.Filled.Fastfood, "fast food burger fries, đồ ăn nhanh"),
            Item("kebab", Icons.Filled.KebabDining, "kebab grill skewer, thịt nướng xiên"),
            Item("takeout", Icons.Filled.TakeoutDining, "takeout food delivery order, đặt đồ ăn mang về"),
            Item("egg", Icons.Filled.Egg, "egg, trứng"),
            Item("cake", Icons.Filled.Cake, "cake birthday dessert, bánh ngọt sinh nhật"),
            Item("cookie", Icons.Filled.Cookie, "cookie snack biscuit, bánh quy ăn vặt"),
            Item("ice_cream", Icons.Filled.Icecream, "ice cream, kem"),
            Item("wine", Icons.Filled.WineBar, "drink wine, uống rượu vang"),
            Item("beer", Icons.Filled.SportsBar, "drink beer, uống bia"),
            Item("cocktail", Icons.Filled.LocalBar, "cocktail bar drink, quán bar cocktail"),
            Item("liquor", Icons.Filled.Liquor, "liquor alcohol bottle, rượu"),
            Item("cook", Icons.Filled.OutdoorGrill, "cook grill barbecue, nấu ăn nướng"),
            Item("no_food", Icons.Filled.NoFood, "fasting skip meal, nhịn ăn"),
            Item("no_drinks", Icons.Filled.NoDrinks, "no alcohol sober, không uống rượu"),
        )),
        Group(R.string.icon_group_health, listOf(
            Item("pill", Icons.Filled.Medication, "take medicine pill vitamins, uống thuốc vitamin"),
            Item("heart", Icons.Filled.Favorite, "love heart, yêu thương trái tim"),
            Item("heart_rate", Icons.Filled.MonitorHeart, "heart rate blood pressure, nhịp tim huyết áp"),
            Item("medical", Icons.Filled.MedicalServices, "medical checkup, khám bệnh"),
            Item("hospital", Icons.Filled.LocalHospital, "hospital clinic doctor, bệnh viện bác sĩ"),
            Item("vaccine", Icons.Filled.Vaccines, "vaccine injection shot, tiêm vắc xin"),
            Item("bandage", Icons.Filled.Healing, "heal wound bandage, băng vết thương"),
            Item("blood", Icons.Filled.Bloodtype, "blood test donate blood, xét nghiệm máu hiến máu"),
            Item("weight", Icons.Filled.MonitorWeight, "weigh body weight scale, cân nặng"),
            Item("meditate", Icons.Filled.SelfImprovement, "meditate mindfulness calm, thiền định"),
            Item("mind", Icons.Filled.Psychology, "mental health therapy think, sức khỏe tinh thần"),
            Item("spa", Icons.Filled.Spa, "spa massage relax, thư giãn mát xa"),
            Item("sleep", Icons.Filled.Bedtime, "sleep go to bed nap, đi ngủ"),
            Item("wake", Icons.Filled.WbSunny, "wake up morning sun, thức dậy buổi sáng"),
            Item("alarm", Icons.Filled.Alarm, "alarm clock, báo thức"),
            Item("shower", Icons.Filled.Shower, "take a shower, tắm vòi sen"),
            Item("bath", Icons.Filled.Bathtub, "take a bath bathtub, tắm bồn"),
            Item("wash_hands", Icons.Filled.CleanHands, "wash hands hygiene, rửa tay"),
            Item("skincare", Icons.Filled.Face, "skincare face care, chăm sóc da mặt"),
            Item("haircut", Icons.Filled.ContentCut, "haircut cut hair barber, cắt tóc"),
            Item("mask", Icons.Filled.Masks, "wear a mask, đeo khẩu trang"),
            Item("eye", Icons.Filled.Visibility, "eyes vision eye drops, mắt nhỏ mắt"),
            Item("drop", Icons.Filled.WaterDrop, "water drop hydrate, giọt nước"),
            Item("no_smoking", Icons.Filled.SmokeFree, "quit smoking no smoking, bỏ thuốc lá"),
            Item("smoking", Icons.Filled.SmokingRooms, "smoke cigarette, hút thuốc"),
        )),
        Group(R.string.icon_group_sport, listOf(
            Item("fitness", Icons.Filled.FitnessCenter, "gym workout lift weights, tập gym thể hình"),
            Item("run", Icons.AutoMirrored.Filled.DirectionsRun, "run jog running, chạy bộ"),
            Item("walk", Icons.AutoMirrored.Filled.DirectionsWalk, "walk stroll steps, đi bộ"),
            Item("bike", Icons.AutoMirrored.Filled.DirectionsBike, "ride a bike cycling, đạp xe"),
            Item("swim", Icons.Filled.Pool, "swim swimming pool, bơi lội"),
            Item("hike", Icons.Filled.Hiking, "hike hiking trail, leo núi đi bộ đường dài"),
            Item("yoga", Icons.Filled.SportsGymnastics, "yoga stretch gymnastics, tập yoga giãn cơ"),
            Item("martial_arts", Icons.Filled.SportsMartialArts, "martial arts karate boxing, võ thuật"),
            Item("soccer", Icons.Filled.SportsSoccer, "play football soccer, đá bóng"),
            Item("basketball", Icons.Filled.SportsBasketball, "play basketball, bóng rổ"),
            Item("tennis", Icons.Filled.SportsTennis, "play tennis badminton, quần vợt cầu lông"),
            Item("volleyball", Icons.Filled.SportsVolleyball, "play volleyball, bóng chuyền"),
            Item("baseball", Icons.Filled.SportsBaseball, "baseball"),
            Item("golf", Icons.Filled.SportsGolf, "golf"),
            Item("cricket", Icons.Filled.SportsCricket, "cricket"),
            Item("rowing", Icons.Filled.Rowing, "rowing boat, chèo thuyền"),
            Item("kayak", Icons.Filled.Kayaking, "kayak canoe, chèo kayak"),
            Item("surf", Icons.Filled.Surfing, "surfing waves, lướt sóng"),
            Item("skate", Icons.Filled.Skateboarding, "skateboard, trượt ván"),
            Item("ski", Icons.Filled.DownhillSkiing, "ski skiing snow, trượt tuyết"),
            Item("snowboard", Icons.Filled.Snowboarding, "snowboard"),
            Item("sport", Icons.Filled.Sports, "sport exercise training, thể thao tập luyện"),
            Item("trophy", Icons.Filled.EmojiEvents, "win trophy achievement, chiến thắng giải thưởng"),
            Item("timer", Icons.Filled.Timer, "timer stopwatch, bấm giờ"),
        )),
        Group(R.string.icon_group_work, listOf(
            Item("work", Icons.Filled.Work, "work job office, làm việc công ty"),
            Item("code", Icons.Filled.Code, "code programming developer, lập trình viết code"),
            Item("terminal", Icons.Filled.Terminal, "terminal command server deploy, máy chủ triển khai"),
            Item("bug", Icons.Filled.BugReport, "fix bug debug, sửa lỗi"),
            Item("laptop", Icons.Filled.Laptop, "laptop, máy tính xách tay"),
            Item("computer", Icons.Filled.Computer, "computer desktop, máy tính"),
            Item("study", Icons.Filled.School, "study school class, học bài trường học"),
            Item("book", Icons.AutoMirrored.Filled.MenuBook, "read a book, đọc sách"),
            Item("reading", Icons.Filled.AutoStories, "read story novel, đọc truyện"),
            Item("write", Icons.Filled.EditNote, "write notes journal, viết ghi chép nhật ký"),
            Item("article", Icons.AutoMirrored.Filled.Article, "article document report, bài viết tài liệu báo cáo"),
            Item("newspaper", Icons.Filled.Newspaper, "read news newspaper, đọc báo tin tức"),
            Item("idea", Icons.Filled.Lightbulb, "idea brainstorm, ý tưởng"),
            Item("science", Icons.Filled.Science, "science lab research, khoa học nghiên cứu"),
            Item("calculate", Icons.Filled.Calculate, "calculate math accounting, tính toán toán học"),
            Item("translate", Icons.Filled.Translate, "learn language translate, học ngoại ngữ dịch"),
            Item("quiz", Icons.Filled.Quiz, "quiz exam test, kiểm tra thi cử"),
            Item("task", Icons.Filled.TaskAlt, "task done todo, hoàn thành công việc"),
            Item("meeting", Icons.Filled.CoPresent, "meeting presentation, họp thuyết trình"),
            Item("event", Icons.Filled.Event, "event appointment, sự kiện cuộc hẹn"),
            Item("calendar", Icons.Filled.CalendarMonth, "calendar plan schedule, lịch kế hoạch"),
            Item("schedule", Icons.Filled.Schedule, "time schedule clock, thời gian giờ giấc"),
            Item("print", Icons.Filled.Print, "print documents, in tài liệu"),
            Item("draw", Icons.Filled.Draw, "draw sketch design, vẽ thiết kế"),
        )),
        Group(R.string.icon_group_home, listOf(
            Item("home", Icons.Filled.Home, "home house, ở nhà"),
            Item("clean", Icons.Filled.CleaningServices, "clean the house tidy, dọn dẹp lau nhà"),
            Item("laundry", Icons.Filled.LocalLaundryService, "do laundry washing machine, giặt đồ"),
            Item("iron", Icons.Filled.Iron, "iron clothes, ủi quần áo"),
            Item("dishes", Icons.Filled.Countertops, "wash dishes kitchen counter, rửa bát"),
            Item("fridge", Icons.Filled.Kitchen, "fridge kitchen, tủ lạnh nhà bếp"),
            Item("microwave", Icons.Filled.Microwave, "microwave heat food, hâm nóng đồ ăn"),
            Item("bed", Icons.Filled.Bed, "make the bed, dọn giường"),
            Item("sofa", Icons.Filled.Weekend, "relax on sofa weekend, nghỉ ngơi cuối tuần"),
            Item("chair", Icons.Filled.Chair, "chair furniture, ghế nội thất"),
            Item("plant", Icons.Filled.LocalFlorist, "water plants flowers, tưới cây hoa"),
            Item("garden", Icons.Filled.Yard, "garden yard, làm vườn"),
            Item("grass", Icons.Filled.Grass, "mow the lawn grass, cắt cỏ"),
            Item("repair", Icons.Filled.Handyman, "repair fix handyman, sửa chữa"),
            Item("tools", Icons.Filled.Build, "tools build, dụng cụ"),
            Item("plumbing", Icons.Filled.Plumbing, "plumbing pipe, sửa ống nước"),
            Item("electric", Icons.Filled.ElectricalServices, "electricity wiring, điện"),
            Item("trash", Icons.Filled.DeleteSweep, "take out trash garbage, đổ rác"),
            Item("recycle", Icons.Filled.Recycling, "recycle, tái chế"),
            Item("key", Icons.Filled.Key, "keys, chìa khóa"),
            Item("lock", Icons.Filled.Lock, "lock door security, khóa cửa"),
            Item("garage", Icons.Filled.Garage, "garage, nhà để xe"),
            Item("light", Icons.Filled.EmojiObjects, "light bulb lamp, đèn"),
            Item("heater", Icons.Filled.LocalFireDepartment, "heating fire, sưởi ấm"),
        )),
        Group(R.string.icon_group_transport, listOf(
            Item("car", Icons.Filled.DirectionsCar, "drive a car, lái ô tô"),
            Item("bus", Icons.Filled.DirectionsBus, "take the bus, đi xe buýt"),
            Item("train", Icons.Filled.Train, "train railway, đi tàu hỏa"),
            Item("subway", Icons.Filled.Subway, "subway metro, tàu điện ngầm"),
            Item("tram", Icons.Filled.Tram, "tram, xe điện"),
            Item("motorbike", Icons.Filled.TwoWheeler, "ride motorbike, đi xe máy"),
            Item("moped", Icons.Filled.Moped, "moped scooter, xe tay ga"),
            Item("e_scooter", Icons.Filled.ElectricScooter, "electric scooter, xe trượt điện"),
            Item("e_bike", Icons.Filled.ElectricBike, "electric bike, xe đạp điện"),
            Item("taxi", Icons.Filled.LocalTaxi, "take a taxi grab, đi taxi"),
            Item("fuel", Icons.Filled.LocalGasStation, "refuel gas station petrol, đổ xăng"),
            Item("charging", Icons.Filled.EvStation, "charge electric car, sạc xe điện"),
            Item("parking", Icons.Filled.LocalParking, "parking, đỗ xe"),
            Item("flight", Icons.Filled.Flight, "flight airplane fly, đi máy bay"),
            Item("boat", Icons.Filled.DirectionsBoat, "boat ferry, đi thuyền phà"),
            Item("delivery", Icons.Filled.LocalShipping, "delivery shipping package, giao hàng gửi hàng"),
            Item("car_repair", Icons.Filled.CarRepair, "car repair maintenance, sửa xe bảo dưỡng"),
            Item("car_wash", Icons.Filled.LocalCarWash, "car wash, rửa xe"),
            Item("commute", Icons.Filled.Commute, "commute to work, đi làm"),
            Item("traffic", Icons.Filled.Traffic, "traffic jam, kẹt xe"),
        )),
        Group(R.string.icon_group_shopping, listOf(
            Item("shopping", Icons.Filled.ShoppingCart, "go shopping cart, đi mua sắm"),
            Item("bag", Icons.Filled.ShoppingBag, "shopping bag, túi mua sắm"),
            Item("mall", Icons.Filled.LocalMall, "shopping mall, trung tâm thương mại"),
            Item("store", Icons.Filled.Storefront, "store shop, cửa hàng"),
            Item("grocery", Icons.Filled.LocalGroceryStore, "buy groceries supermarket, đi chợ siêu thị"),
            Item("clothes", Icons.Filled.Checkroom, "clothes outfit wardrobe, quần áo"),
            Item("jewelry", Icons.Filled.Diamond, "jewelry diamond, trang sức"),
            Item("gift", Icons.Filled.CardGiftcard, "gift present, quà tặng"),
            Item("sale", Icons.Filled.Sell, "sell sale discount, bán hàng giảm giá"),
            Item("receipt", Icons.Filled.Receipt, "receipt bill, hóa đơn"),
            Item("money", Icons.Filled.Payments, "money cash payment, tiền mặt thanh toán"),
            Item("card", Icons.Filled.CreditCard, "credit card pay, thẻ tín dụng"),
            Item("bank", Icons.Filled.AccountBalance, "bank, ngân hàng"),
            Item("wallet", Icons.Filled.AccountBalanceWallet, "wallet budget, ví ngân sách"),
            Item("savings", Icons.Filled.Savings, "save money savings, tiết kiệm tiền"),
            Item("dollar", Icons.Filled.AttachMoney, "earn money salary, kiếm tiền lương"),
            Item("exchange", Icons.Filled.CurrencyExchange, "currency exchange transfer, đổi tiền chuyển khoản"),
            Item("invest", Icons.AutoMirrored.Filled.TrendingUp, "invest stocks, đầu tư chứng khoán"),
            Item("paid", Icons.Filled.Paid, "pay bills, trả tiền"),
            Item("bill", Icons.Filled.RequestQuote, "invoice quote bill, hóa đơn báo giá"),
        )),
        Group(R.string.icon_group_fun, listOf(
            Item("movie", Icons.Filled.Movie, "watch a movie film, xem phim"),
            Item("theater", Icons.Filled.TheaterComedy, "theater comedy show, nhà hát hài kịch"),
            Item("tv", Icons.Filled.Tv, "watch tv television, xem tivi"),
            Item("music", Icons.Filled.MusicNote, "listen to music song, nghe nhạc"),
            Item("headphones", Icons.Filled.Headphones, "headphones listen, tai nghe"),
            Item("podcast", Icons.Filled.Podcasts, "podcast, nghe podcast"),
            Item("radio", Icons.Filled.Radio, "radio"),
            Item("piano", Icons.Filled.Piano, "play piano instrument, chơi đàn piano"),
            Item("game", Icons.Filled.SportsEsports, "play games gaming, chơi game"),
            Item("videogame", Icons.Filled.VideogameAsset, "video game console, máy chơi game"),
            Item("puzzle", Icons.Filled.Extension, "puzzle, giải đố"),
            Item("toys", Icons.Filled.Toys, "toys play, đồ chơi"),
            Item("casino", Icons.Filled.Casino, "dice board game, xúc xắc trò chơi"),
            Item("photo", Icons.Filled.PhotoCamera, "take photos camera, chụp ảnh"),
            Item("paint", Icons.Filled.Palette, "paint art colors, vẽ tranh nghệ thuật"),
            Item("brush", Icons.Filled.Brush, "brush painting, cọ vẽ"),
            Item("ticket", Icons.Filled.LocalActivity, "ticket concert event, vé sự kiện"),
            Item("party", Icons.Filled.Celebration, "party celebrate, tiệc ăn mừng"),
            Item("nightlife", Icons.Filled.Nightlife, "nightlife club drinks, đi bar đêm"),
            Item("library", Icons.Filled.LocalLibrary, "library, thư viện"),
        )),
        Group(R.string.icon_group_social, listOf(
            Item("call", Icons.Filled.Call, "phone call, gọi điện thoại"),
            Item("video_call", Icons.Filled.VideoCall, "video call, gọi video"),
            Item("chat", Icons.AutoMirrored.Filled.Chat, "chat message text, nhắn tin"),
            Item("forum", Icons.Filled.Forum, "discussion talk, thảo luận trò chuyện"),
            Item("mail", Icons.Filled.Email, "email mail, gửi thư email"),
            Item("people", Icons.Filled.People, "meet friends, gặp bạn bè"),
            Item("group", Icons.Filled.Groups, "group team community, nhóm cộng đồng"),
            Item("family", Icons.Filled.FamilyRestroom, "family parents, gia đình bố mẹ"),
            Item("child", Icons.Filled.ChildCare, "child baby kids, con cái em bé"),
            Item("stroller", Icons.Filled.ChildFriendly, "baby stroller, xe đẩy em bé"),
            Item("elderly", Icons.Filled.Elderly, "visit grandparents elderly, thăm ông bà"),
            Item("pet", Icons.Filled.Pets, "pet dog cat, thú cưng chó mèo"),
            Item("love", Icons.Filled.VolunteerActivism, "volunteer help charity, tình nguyện từ thiện"),
            Item("handshake", Icons.Filled.Handshake, "deal agreement handshake, thỏa thuận bắt tay"),
            Item("add_friend", Icons.Filled.PersonAdd, "make a new friend, kết bạn"),
            Item("church", Icons.Filled.Church, "church pray, nhà thờ cầu nguyện"),
            Item("mosque", Icons.Filled.Mosque, "mosque pray, nhà thờ hồi giáo"),
            Item("temple", Icons.Filled.TempleBuddhist, "temple pagoda, đi chùa"),
        )),
        Group(R.string.icon_group_travel, listOf(
            Item("luggage", Icons.Filled.Luggage, "travel luggage suitcase, du lịch hành lý"),
            Item("beach", Icons.Filled.BeachAccess, "beach sea, đi biển"),
            Item("park", Icons.Filled.Park, "park tree, công viên"),
            Item("forest", Icons.Filled.Forest, "forest woods, rừng"),
            Item("mountain", Icons.Filled.Landscape, "mountain landscape, núi phong cảnh"),
            Item("terrain", Icons.Filled.Terrain, "hills terrain, đồi"),
            Item("camping", Icons.Filled.Cabin, "camping cabin, cắm trại"),
            Item("hotel", Icons.Filled.Hotel, "hotel stay, khách sạn"),
            Item("map", Icons.Filled.Map, "map directions, bản đồ"),
            Item("explore", Icons.Filled.Explore, "explore adventure, khám phá"),
            Item("world", Icons.Filled.Public, "world trip abroad, nước ngoài thế giới"),
            Item("attractions", Icons.Filled.Attractions, "amusement park attractions, khu vui chơi"),
            Item("sailing", Icons.Filled.Sailing, "sailing, đi thuyền buồm"),
            Item("sunset", Icons.Filled.WbTwilight, "sunset sunrise, hoàng hôn"),
            Item("night", Icons.Filled.NightsStay, "night moon, đêm trăng"),
            Item("snow", Icons.Filled.AcUnit, "cold winter snow, mùa đông lạnh"),
            Item("storm", Icons.Filled.Thunderstorm, "storm rain weather, mưa bão thời tiết"),
            Item("umbrella", Icons.Filled.Umbrella, "umbrella rain, ô dù"),
            Item("waves", Icons.Filled.Water, "river lake water, sông hồ"),
            Item("eco", Icons.Filled.Eco, "nature green environment, thiên nhiên môi trường"),
            Item("farm", Icons.Filled.Agriculture, "farm tractor, nông trại"),
        )),
        Group(R.string.icon_group_other, listOf(
            Item("check", Icons.Filled.CheckCircle, "done check, xong"),
            Item("star", Icons.Filled.Star, "star favorite, ngôi sao yêu thích"),
            Item("flag", Icons.Filled.Flag, "goal milestone flag, mục tiêu cột mốc"),
            Item("bookmark", Icons.Filled.Bookmark, "bookmark save, đánh dấu"),
            Item("pin", Icons.Filled.PushPin, "pin important, ghim quan trọng"),
            Item("bolt", Icons.Filled.Bolt, "energy power, năng lượng"),
            Item("fire", Icons.Filled.Whatshot, "streak hot, chuỗi ngày nóng"),
            Item("happy", Icons.Filled.SentimentSatisfied, "happy mood good, vui vẻ tâm trạng tốt"),
            Item("sad", Icons.Filled.SentimentDissatisfied, "sad mood bad, buồn tâm trạng tệ"),
            Item("thumb_up", Icons.Filled.ThumbUp, "like good, thích tốt"),
            Item("thumb_down", Icons.Filled.ThumbDown, "dislike bad, không thích"),
            Item("warning", Icons.Filled.Warning, "warning problem, cảnh báo vấn đề"),
            Item("info", Icons.Filled.Info, "information, thông tin"),
            Item("hourglass", Icons.Filled.HourglassEmpty, "wait waiting, chờ đợi"),
            Item("phone", Icons.Filled.Smartphone, "phone screen time smartphone, điện thoại"),
            Item("wifi", Icons.Filled.Wifi, "internet wifi online, mạng internet"),
            Item("battery", Icons.Filled.BatteryFull, "charge phone battery, sạc pin"),
            Item("cloud", Icons.Filled.Cloud, "cloud backup weather, đám mây sao lưu"),
        )),
    )

    val all: List<Item> = groups.flatMap { it.icons }
    private val byKey = all.associateBy { it.key }

    fun icon(key: String?): ImageVector? = key?.let { byKey[it]?.vector }
}
