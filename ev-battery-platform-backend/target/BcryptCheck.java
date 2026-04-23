public class BcryptCheck {
  public static void main(String[] args) {
    String raw = "123456";
    String hash = "$2a$10$L2hcre1ZhbGo86QSp6bbMOQ7npm26m8nt4QpXKTk12.vUMug6EgyW";
    org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    System.out.println(encoder.matches(raw, hash));
  }
}
