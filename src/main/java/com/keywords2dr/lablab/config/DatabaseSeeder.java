//package com.keywords2dr.lablab.config;
//
//import com.keywords2dr.lablab.entity.Chemical;
//import com.keywords2dr.lablab.entity.DataAlias;
//import com.keywords2dr.lablab.entity.Room;
//import com.keywords2dr.lablab.entity.RoomInventory;
//import com.keywords2dr.lablab.repository.ChemicalRepository;
//import com.keywords2dr.lablab.repository.DataAliasRepository;
//import com.keywords2dr.lablab.repository.RoomInventoryRepository;
//import com.keywords2dr.lablab.repository.RoomRepository;
//import com.keywords2dr.lablab.service.DataNormalizationService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DatabaseSeeder implements CommandLineRunner {
//
//    private final RoomRepository roomRepository;
//    private final ChemicalRepository chemicalRepository;
//    private final RoomInventoryRepository inventoryRepository;
//
//    // Thêm các dependency để xử lý từ điển
//    private final DataAliasRepository dataAliasRepository;
//    private final DataNormalizationService normalizationService;
//
//    @Override
//    @Transactional
//    public void run(String... args) throws Exception {
//
//        // 0. SEED BẢNG TỪ ĐIỂN TRƯỚC (NẾU TRỐNG)
//        if (dataAliasRepository.count() == 0) {
//            log.info("📚 Tạo dữ liệu Từ điển chuẩn hóa...");
//            dataAliasRepository.save(new DataAlias(null, "PACKAGING", "thuỷ tinh", "Thủy tinh"));
//            dataAliasRepository.save(new DataAlias(null, "PACKAGING", "nhua", "Nhựa"));
//            dataAliasRepository.save(new DataAlias(null, "UNIT", "lít", "L"));
//            dataAliasRepository.save(new DataAlias(null, "UNIT", "lit", "L"));
//            dataAliasRepository.save(new DataAlias(null, "UNIT", "gam", "g"));
//
//            // Ép Service load lại dữ liệu ngay lập tức để dùng luôn cho vòng lặp Hóa chất bên dưới
//            normalizationService.refreshCache();
//        }
//
//        if (chemicalRepository.count() > 0) {
//            log.info("✅ Dữ liệu Hóa chất đã tồn tại. Bỏ qua chạy Seeder.");
//            return;
//        }
//
//        log.info("🚀 Bắt đầu tạo ĐẦY ĐỦ 18 Phòng và 30 Hóa chất theo file Excel...");
//
//        // ==========================================
//        // 1. TẠO TẤT CẢ CÁC PHÒNG LAB
//        // ==========================================
//        String[] roomNames = {
//                "103", "104", "105", "111", "202", "203", "205", "206", "207",
//                "209", "302", "304", "307", "308", "309", "310", "311", "312"
//        };
//
//        Map<String, Room> roomMap = new HashMap<>();
//        for (String rName : roomNames) {
//            Room r = new Room();
//            r.setRoomName(rName);
//            r.setDescription("Phòng Lab " + rName);
//            r.setIsActive(true);
//            roomMap.put(rName, roomRepository.save(r));
//        }
//
//        // ==========================================
//        // 2. MẢNG DỮ LIỆU 30 DÒNG HÓA CHẤT TỪ EXCEL
//        // ==========================================
//        String[][] rawData = {
//                {"HC_001", "Iodic acid", "HIO3", "Germany", "310", "thủy tinh", "5", "500", "g", ""},
//                {"HC_002", "(3-Amoniprpropyl)", "C9H23NO3Si", "USA", "302", "Thủy tinh", "1", "75", "ml", "thầy ban"},
//                {"HC_003", "1-(2-Pyridylazo)-2-naphthol (PAN)", "C15H11N3O", "Germany", "302", "Thủy tinh", "2", "4.5", "g", ""},
//                {"HC_004", "1,10-phenanthroline", "C12H8N2", "Japan", "310", "nhựa", "2", "20", "g", ""},
//                {"HC_005", "1,10-Phenanthroline monohydrate", "C12H8N2.H2O", "China", "311", "nhựa", "1", "1.8", "g", ""},
//                {"HC_006", "1-Methyl-2-pyrrolidone", "C5H9NO", "Germany", "310", "nhựa", "1", "300", "ml", "Cô Lan"},
//                {"HC_007", "1-Naphtholbenzein", "C27H18O2", "China", "105", "thủy tinh", "1", "20", "g", ""},
//                {"HC_008", "1-Naphthyl methacrylate", "C14H12O2", "Germany", "105", "thủy tinh", "1", "450", "ml", ""},
//                {"HC_009", "1-Naphthylacetic acid", "C12H10O2", "Germany/India", "205", "nhựa", "2", "7", "g", ""},
//                {"HC_010", "1-Napthol", "C10H7OH", "China", "304", "Thủy tinh", "2", "50", "g", ""},
//                {"HC_011", "2,4-Dichloro-phenoxyacetic acid", "C8H6Cl2O3", "Germany", "205", "nhựa", "1", "25", "g", ""},
//                {"HC_012", "2,4-Dichloro-phenoxyacetic acid", "C8H6Cl2O3", "China", "205", "nhựa", "3", "700", "g", ""},
//                {"HC_013", "2,6-Dichlorophenolindophenol sodium salt dihydrate", "C12H6Cl2NO2Na.2H2O", "China", "105", "nhựa", "1", "5", "g", ""},
//                {"HC_014", "2-4-Dinitrophenylhydrazine", "C6H6N4O4", "China", "304", "thủy tinh", "1", "400", "g", ""},
//                {"HC_015", "2-Butanol", "C4H10O", "China", "304", "Thủy tinh", "1", "500", "ml", ""},
//                {"HC_016", "2-Furyl methyl ketone", "C6H6O2", "Germany", "304", "nhựa", "1", "20", "g", ""},
//                {"HC_017", "2-methoxyethanol", "C3H8O2", "China", "111", "thủy tinh", "1", "450", "ml", ""},
//                {"HC_018", "2-Naphthol", "C10H7OH", "China", "304", "Thủy tinh", "2", "60", "g", ""},
//                {"HC_019", "2-Naphthol", "C10H7OH", "China", "304", "Nhựa", "4", "110", "g", ""},
//                {"HC_020", "3-(Trimethoxysilyl)-1-propanthiol", "C6H10O3SSi", "China", "310", "Thủy tinh", "1", "90", "ml", ""},
//                {"HC_021", "4-Mercaptobenzoic acid", "C7H6O2S", "China", "111", "thủy tinh", "1", "3", "g", ""},
//                {"HC_022", "4-Nitroaniline", "C6H6N2O2", "Germany", "304", "Thủy tinh", "1", "200", "g", ""},
//                {"HC_023", "5-Sulfosalicylic acid dihydrate", "C7H6O6S.2H2O", "China", "203", "thủy tinh", "1", "70", "g", ""},
//                {"HC_024", "5-Sulfosalicylic acid dihydrate", "C7H6O6S.2H2O", "China", "302", "nhựa", "1", "70", "g", ""},
//                {"HC_025", "5-Sulfosalicylic acid dihydrate", "C7H6O6S.2H2O", "China", "311", "Thủy tinh", "2", "150", "g", ""},
//                {"HC_026", "5-Sulfosalicylic acid dihydrate", "C7H6O6S.2H2O", "China", "203", "Nhựa", "5", "210", "g", ""},
//                {"HC_027", "5-Sulfosalicylic acid dihydrate", "C7H6O6S.2H2O", "China", "304", "nhựa", "4", "50", "g", ""},
//                {"HC_028", "6-(Furfuryl-amino)purine", "C10H9N5O", "Germany", "205", "thủy tinh", "2", "2", "g", ""},
//                {"HC_029", "6-(Furfuryl-amino)purine", "C10H9N5O", "Switzerland", "205", "nhựa", "1", "1", "g", ""},
//                {"HC_030", "6-Benzyladenine", "C12H11N5", "India", "205", "thủy tinh", "1", "5", "g", ""}
//        };
//
//        // ==========================================
//        // 3. VÒNG LẶP CHÈN DỮ LIỆU TỰ ĐỘNG
//        // ==========================================
//        for (String[] data : rawData) {
//            BigDecimal totalQty = new BigDecimal(data[7]);
//            int packageCount = Integer.parseInt(data[6]);
//
//            BigDecimal amountPerPkg = packageCount > 0
//                    ? totalQty.divide(new BigDecimal(packageCount), 2, RoundingMode.HALF_UP)
//                    : BigDecimal.ZERO;
//
//            Chemical c = new Chemical();
//            c.setItemCode(data[0]);
//            c.setName(data[1]);
//            c.setCategoryType("CHEMICAL");
//            c.setFormula(data[2]);
//            c.setSupplier(data[3]);
//
//            // ĐÃ SỬA: Dùng hàm mới và truyền thêm aliasType
//            c.setPackaging(normalizationService.normalizeAndLearn(data[5], "PACKAGING"));
//            c.setUnit(normalizationService.normalizeAndLearn(data[8], "UNIT"));
//
//            c.setAmountPerPackage(amountPerPkg);
//
//            c = chemicalRepository.save(c);
//
//            Room targetRoom = roomMap.get(data[4]);
//            if (targetRoom != null) {
//                RoomInventory inv = new RoomInventory();
//                inv.setRoom(targetRoom);
//                inv.setItem(c);
//                inv.setPackageCount(packageCount);
//                inv.setTotalQuantity(totalQty);
//                inv.setLockedQuantity(BigDecimal.ZERO);
//                inv.setNote(data[9]);
//                inventoryRepository.save(inv);
//            }
//        }
//
//        log.info("🎉 Import thành công tất cả phòng và 30 loại hóa chất! Database đã sẵn sàng.");
//    }
//}