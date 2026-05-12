/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import ghidra.app.util.bin.ByteArrayProvider;
import ghidra.app.util.opinion.LoadResults;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra_snes.common.RomMapType;
import ghidra_snes.common.SnesRomHeader;
import ghidra_snes.common.SnesRomHeaderDetector;
import ghidra_snes.common.rom.Checksum;
import ghidra_snes.testing.ProgramFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionalTestRealDataTest {
  private static final Path REAL_DATA_DIR = Path.of("src/test/resources/data");

  record DataBank(int chunkId, int bankId, String bankSha256) {}

  record RealDataCase(
    String sha256,
    RomMapType romMapType,
    int bankSize,
    int banks,
    int checksum,
    List<DataBank> banksData) {
      String shortSha256() { return sha256.substring(0, 8) + "..."; }
      Path path() { return REAL_DATA_DIR.resolve(sha256); }
  }

  private static final List<RealDataCase> REAL_DATA = List.of(
    // Smashing The Stack For Fun And Profit
    new RealDataCase(
      "smashing_the_stack.sfc",
      RomMapType.LoROM,
      0x8000,
      2,
      0xD07B,
      List.of(
        new DataBank(0x00, 0x80, "6250c714cef198ffb512c6fe6286903bed98fdaa496692ec6aa429d70b40fefe"),
        new DataBank(0x01, 0x81, "c89789b7187a1053e8acb3ec35ae6b15de9b3d2a63128604e862e91de76a7cee")
      )
    ),
    // A plumber all over the world
    new RealDataCase(
      "5cc54b1e5c8d3c7701a5e20514145c3b36f15f26fe0a4fe6d2e43677e4b4eda9",
      RomMapType.LoROM,
      0x8000,
      16,
      0xc536,
      List.of(
        new DataBank(0x00, 0x80, "7af10b2610df778bb1012f191ef22de22971b7a151eacc03dc639be4dcc8bcf8"),
        new DataBank(0x0F, 0x8F, "803e19581463d84f43fb071c2314f1e8a227150404e654a0890fa5889714e459")

      )),
    // Women don't get enough representation in Sci-Fi
    new RealDataCase(
      "12b77c4bc9c1832cee8881244659065ee1d84c70c3d29e6eaf92e6798cc2ca72",
      RomMapType.LoROM,
      0x8000,
      96,
      0xf8df,
      List.of(
        new DataBank(0x00, 0x80, "69ac00e1f983a42503fb8f3ce481ec3f7e39bb5c1408de3cf71b3cfffca48c75"),
        new DataBank(0x5f, 0xdf, "d1042bf8983d2d8251d25b7ff20a5e74f0f87702b656192cf68eea193f8fa264")
      )),
    // A super robot from the future suffers a copy protection error
    new RealDataCase(
      "3e1209f473bff8cd4bcbf71d071e7f8df17a2d564e9a5c4c427ee8198cebb615",
      RomMapType.LoROM,
      0x8000,
      48,
      0x55fb,
      List.of(
        new DataBank(0x00, 0x80, "29f2dbe65d575f3112ff7ba8cfa4baff81b62cf84e55a40fac6c6c9d6dfb4982"),
        new DataBank(0x2f, 0xaf, "7976b2efdc9497c10702616d0a2903d0ae7977f0159610d53c5e7a0c680b1061")
      )),
    // That same robot, but now he's rocking in Japan
    new RealDataCase(
      "2626625f29e451746c8762f9e313d1140457fe68b27d36ce0cbee9b5c5be9743",
      RomMapType.LoROM,
      0x8000,
      48,
      0x6569,
      List.of(
        new DataBank(0x00, 0x80, "b3c6aaf33de165804ac071a8852f1d78a38ad11e8b46435c594332ae1faa01eb"),
        new DataBank(0x2f, 0xaf, "7976b2efdc9497c10702616d0a2903d0ae7977f0159610d53c5e7a0c680b1061")
      )),
    //  That same robot comes back, IN AMERICA
    new RealDataCase(
      "f3246755f608a1e1dc9c848b61da3b824c7853b29b3be40df6fc7f2793a887ed",
      RomMapType.LoROM,
      0x8000,
      48,
      0x09b7,
      List.of(
        new DataBank(0x00, 0x80, "e645f36df4eab86c32369d6caac90464e84dd51e346a51ea16ca6614bf613e71"),
        new DataBank(0x2f, 0xaf, "c5449e03695bc53df0f64e99394e202ccfe7a2dd9b36d7e5ddf55fe6180b2dd7")
      )),
    // Japanese, pink, and shaped like a friend
    new RealDataCase(
      "0c637df08d73e13e8e22fafbfe2b9196e08670ac27dbdd43330289c7fbce74e1",
      RomMapType.SA_1,
      0x10000,
      64,
      0x2233,
      List.of(
        new DataBank(0x00, 0xC0, "bef107343ad633b2519f182490e80a0300701d59de4658e0761327e37d454041"),
        new DataBank(0x3f, 0xff, "7ce9fdb8cf93c27ef7584e19da7c10a606b5ff39a7726c705243d0f5669e14d6")
      )),
    // A plumber tries its hands at a beloved Japanese genre
    new RealDataCase(
      "5d6aa9daec8525495510cda7cbd0bf476466dc4d8d86c28420e31715db351c64",
      RomMapType.SA_1,
      0x10000,
      64,
      0x4575,
      List.of(
        new DataBank(0x00, 0xC0, "6f6e08a9a64621e957f1eab8b02b399523dd05a392ca017f69d77670b4904d05"),
        new DataBank(0x3f, 0xff, "9f7202b8450e93d793b1a9d99759bd178d2a0b214d87a808844fd8557b21041d")
      )),
    // French attempt to remake the Earth in our image
    new RealDataCase(
      "5d0a234a2fcb343d169206d9d7d578507c44f800ead9cc9ccfa0b1d4cb1cc9e5",
      RomMapType.HiROM,
      0x10000,
      64,
      0x3ced,
      List.of(
        new DataBank(0x00, 0xC0, "7588c4d8dd1eff1437f6ea1bb428e75614521f3ba66bf98b6cb615560a8ae458"),
        new DataBank(0x3f, 0xff, "02494be83093738694e3df917ed4d6eb2f5dddb4a4997a53ee8f1decf94cee29")
      )),
    // Holidays in Jipang (Episode Zero)
    new RealDataCase(
      "8620203da71d32d017bb21f542864c1d90705b87eb67815d06b43f09120318aa",
      RomMapType.SPC7110,
      0x10000,
      80,
      0xde89,
      List.of(

      )),
    // Big monsters, Electric Boogaloo
    new RealDataCase(
      "b8f31f3292609890e6321e6d925d60266c93e91f1a0e5999f7473adc7bb265f3",
      RomMapType.ExHiROM,
      0x10000,
      80,
      0x8528,
      List.of(
        new DataBank(0x00, 0xC0, "36681ba8ff9db3ec0ce8d62ef55c6e5ef600dda685e1b231fc679fb26ae22643"),
        new DataBank(0x3F, 0xff, "f5ed6f87146b867093138538b4c770ff3ecebc382a7769dd089a320a24e085b6"),
        new DataBank(0x40, 0x40, "87a79642ab0561f6bd87a75ff25f47e7baa674b27e8e1ae355021aae7a1db63b"),
        new DataBank(0x4f, 0x4f, "1ec9832638757201c75dfbbbf65c1652ac982dfc6ff42495a6df15925b2eb2ca")
      )),
    // These stories are fantastic
    new RealDataCase(
      "77b2d5450ce3c87185f913c2584673530c13dfbe8cc433b1e9fe5e9a653bf7d5",
      RomMapType.ExHiROM,
      0x10000,
      96,
      0x7c57,
      List.of(
        new DataBank(0x00, 0xC0, "1a406424ab7902e7410c276d804b537a820f85ad63ccff995e8f30c6163924bd"),
        new DataBank(0x3f, 0xff, "90263272f01e90e4d0f414df21350c0250af6882a92e6a9e16ebf050e214bc05"),
        new DataBank(0x40, 0x40, "b9f68bc2a52793570f831a5f74d8dccd75ab19b84f02057f87df9ef349f34cb6"),
        new DataBank(0x5f, 0x5f, "176947738c0f1453887282c3b6e5c9a27a85104984b8e37d2751959a5deb9d10")
      )),
    // A sky to swim for
    new RealDataCase(
      "efae37be832d0ea1490784d57bef00761a8bf0b5bcef9c23f558e063441c3876",
      RomMapType.S_DD1,
      0x10000,
      96,
      0x13b8,
      List.of(
        new DataBank(0x00, 0xC0, "9c318210571b5033a6a20135e091b229ba4e34529ba9925d57b1bd28620aaac4"),
        new DataBank(0x3f, 0xff, "3e7de5cd7c09a6a7ac9f45dcf7e0ddb64a4061119dd52342425031895fe7f402"),
        new DataBank(0x40, 0x40, "c8589e4dc632df43b37b9f22e8d70bc77eb4f244f096440a608d8598840f9249"),
        new DataBank(0x5f, 0x5f, "ce2e6458ec2773d172277126bdaa69c778b94814c861fc8901ce4a51577d4b3b")
      ))
    );

    static Stream<Arguments> realDataAutoDetectCases() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " has one auto-detected header", data)));
    }

  static Stream<Arguments> realDataTypeCases() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " is of type " + data.romMapType(), data)));
  }

  static Stream<Arguments> realDataCopierHeader() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " has no copier header", data)));
  }

  static Stream<Arguments> realDataSplitChunksInCorrectSize() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " chunks are " + data.bankSize() + " bytes long", data)));
    }

  static Stream<Arguments> realDataChunkCases() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " is made of " + data.banks() + " chunks", data)));
  }

  static Stream<Arguments> realDataChecksums() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(String.format("%s has checksum 0x%04x", data.shortSha256(), data.checksum()), data)));
  }

  static Stream<Arguments> realDataBankCases() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " is checked against " + data.banksData().size() + " mappings", data)));
  }

  static Stream<Arguments> realDataHeaderMirrorTitleCases() {
    return REAL_DATA.stream().map(data ->
      Arguments.of(Named.of(data.shortSha256() + " mirrors header title at $00:FFC0", data)));
  }

  @Order(0)
  @DisplayName("Auto-detects the header")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataAutoDetectCases")
  void autoDetectsHeaderOnRealData(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    ByteArrayProvider provider = new ByteArrayProvider(Files.readAllBytes(data.path()));
    assertDoesNotThrow(() -> SnesRomHeaderDetector.autoDetectRomHeader(provider));
  }

  @Order(1)
  @DisplayName("Detects type on real data")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataTypeCases")
  void detectsTypeOnRealData(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    ByteArrayProvider provider = new ByteArrayProvider(Files.readAllBytes(data.path()));
    SnesRomHeader header = SnesRomHeaderDetector.autoDetectRomHeader(provider);
    assertEquals(data.romMapType(), header.romMapType());
  }

  @Order(2)
  @DisplayName("Detects copier header presence")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataCopierHeader")
  void detectsCopierHeader(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    ByteArrayProvider provider = new ByteArrayProvider(Files.readAllBytes(data.path()));
    SnesCartridge cartridge = new SnesCartridge(provider);
    assertEquals(false, cartridge.hasCopierHeader());
  }

  @Order(3)
  @DisplayName("Split chunks from real data")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataSplitChunksInCorrectSize")
  void realDataSplitChunksInCorrectSize(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    ByteArrayProvider provider = new ByteArrayProvider(Files.readAllBytes(data.path()));
    SnesCartridge cartridge = new SnesCartridge(provider);
    var mapChunksIterator = cartridge.getRomMapType().mappingChunks(cartridge.getRomSizeBytes());
    var chunks = StreamSupport.stream(mapChunksIterator.spliterator(), false).toList();
    var uniqueSizes = chunks.stream().map(RomMapType.MappingChunk::requestedSize).distinct().toList();

    assertEquals(1, uniqueSizes.size());
    assertEquals(data.bankSize(), uniqueSizes.get(0));
  }

  @Order(4)
  @DisplayName("Extracts chunks from real data")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataChunkCases")
  void unpacksChunksOnRealData(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    ByteArrayProvider provider = new ByteArrayProvider(Files.readAllBytes(data.path()));
    SnesCartridge cartridge = new SnesCartridge(provider);
    var mapChunksIterator = cartridge.getRomMapType().mappingChunks(cartridge.getRomSizeBytes());
    var chunks = StreamSupport.stream(mapChunksIterator.spliterator(), false).toList();

    assertEquals(data.banks(), chunks.size());
  }


  @Order(5)
  @DisplayName("Calculates checksum from real data")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataChecksums")
  void calculatesChecksumOnRealData(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    ByteArrayProvider provider = new ByteArrayProvider(Files.readAllBytes(data.path()));
    SnesCartridge cartridge = new SnesCartridge(provider);
    SnesRomHeader header = cartridge.getRomHeader();
    long romOffset = cartridge.getRomOffset();
    long romSize = provider.length() - romOffset;
    int checksum = Checksum.snesChecksum(
      provider,
      romOffset,
      romSize,
      header.romSize(),
      header.romMapType());
    assertEquals(data.checksum(), checksum);
  }

  @Order(6)
  @DisplayName("Maps chunks to the correct banks")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataBankCases")
  void chunksAreMappedInProperPlacs(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    byte[] romData = Files.readAllBytes(data.path());
    ByteArrayProvider provider = new ByteArrayProvider(romData);
    SnesCartridge cartridge = new SnesCartridge(provider);
    var mapChunksIterator = cartridge.getRomMapType().mappingChunks(cartridge.getRomSizeBytes());
    var chunks = StreamSupport.stream(mapChunksIterator.spliterator(), false).toList();

    Assumptions.assumeTrue(data.banksData().size() > 0, () -> "Missing data bank tests: " + data.sha256());

    for (DataBank dataBank : data.banksData()) {
      var chunk = chunks.get(dataBank.chunkId());
      assertEquals(dataBank.bankId(), chunk.bank());

      int chunkStart = Math.toIntExact(chunk.fileOffset());
      int chunkEnd = Math.toIntExact(chunk.fileOffset() + chunk.requestedSize());
      byte[] chunkBytes = Arrays.copyOfRange(romData, chunkStart, chunkEnd);

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String dataSha256 = HexFormat.of().formatHex(digest.digest(chunkBytes));

      assertEquals(dataBank.bankSha256(), dataSha256);
    }
  }

  @Order(7)
  @DisplayName("Mirrors header title at $00:FFC0")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("realDataHeaderMirrorTitleCases")
  void mirrorsHeaderTitleAtBank00Ffc0(RealDataCase data) throws Exception {
    Assumptions.assumeTrue(Files.exists(data.path()), () -> "Missing real data: " + data.sha256());

    byte[] romData = Files.readAllBytes(data.path());
    SnesRomHeader header;
    try (ByteArrayProvider provider = new ByteArrayProvider(data.sha256() + ".sfc", romData)) {
      header = SnesRomHeaderDetector.autoDetectRomHeader(provider);
    }

    byte[] mirroredTitle = new byte[21];
    try (LoadResults<Program> loadResults = ProgramFactory.loadSnesRom(data.sha256(), romData)) {
      loadResults.getPrimary().apply(program -> {
        try {
          int read =
              program.getMemory().getBytes(
                  program.getAddressFactory().getDefaultAddressSpace().getAddress(0x00ffc0L),
                  mirroredTitle);
          assertEquals(mirroredTitle.length, read);
        } catch (MemoryAccessException e) {
          throw new AssertionError("Cannot read mirrored ROM title at $00:FFC0", e);
        }
      });
    }

    assertArrayEquals(header.titleBytes(), mirroredTitle);
  }
}
