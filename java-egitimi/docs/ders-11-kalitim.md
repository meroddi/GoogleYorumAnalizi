# Ders 11 — Kalıtım (inheritance), super, override, Object metotları

> Kod: [`m02oop/ders11/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders11/) — `Account.java`, `CheckingAccount.java`, `SavingsAccount.java`, `InheritanceDemo.java`

## 1. Bu ne?

Kalıtım, bir sınıfın başka bir sınıfın alanlarını ve metotlarını devralmasıdır:

```java
public class CheckingAccount extends Account { ... }
```

**Ne zaman kullanılır?** Sadece gerçek bir **IS-A (bir türüdür)** ilişkisi varsa:

- Vadesiz hesap **bir** hesaptır ✓
- Müşteri **bir** hesap değildir ✗ (müşterinin hesabı *vardır* — HAS-A, kalıtım değil alan)

Yaygın hata: "iki sınıfta aynı kod var, kalıtımla tekrarı önleyeyim." Bu yanlış sebeptir. Kod paylaşımı için kompozisyon (bir sınıfı alan olarak tutmak) kullanılır.

## 2. Nasıl çalışır?

```java
public class Account {
    protected final String accountNo;   // protected: bu sınıf + alt sınıflar görür
    protected Money balance;

    public final void withdraw(Money amount) {       // AKIŞ sabit
        Money totalCost = amount.add(withdrawalFee(amount));
        if (!canWithdraw(totalCost)) throw new IllegalStateException("Cekim reddedildi...");
        this.balance = balance.subtract(totalCost);
    }

    protected boolean canWithdraw(Money amount) { return !amount.isGreaterThan(balance); }
    protected Money withdrawalFee(Money amount)  { return Money.zeroTry(); }
}
```

Bu kalıbın adı **template method**: akış üst sınıfta sabit, adımlar alt sınıfta değişken. `withdraw` `final` çünkü alt sınıflar akışı bozmamalı, sadece kuralları özelleştirmeli.

### Alt sınıflar kuralı değiştiriyor

```java
public class CheckingAccount extends Account {
    private final Money overdraftLimit;

    public CheckingAccount(String accountNo, String ownerName, Money initial, Money overdraftLimit) {
        super(accountNo, ownerName, initial);      // ÜST constructor — ilk satır olmak ZORUNDA
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    protected boolean canWithdraw(Money amount) {
        return !amount.isGreaterThan(balance.add(overdraftLimit));   // eksiye düşebilir
    }
}
```

```java
public class SavingsAccount extends Account {
    @Override
    protected Money withdrawalFee(Money amount) { return amount.percentage("0.01"); }  // %1 ceza

    @Override
    protected boolean canWithdraw(Money amount) {
        if (withdrawalCount >= 3) return false;          // ayda en fazla 3 çekim
        boolean allowed = super.canWithdraw(amount);     // üst sınıfın kuralı + kendi kuralı
        if (allowed) withdrawalCount++;
        return allowed;
    }
}
```

`@Override` anotasyonu zorunlu değil ama **her zaman yaz**: metot adını yanlış yazarsan derleyici uyarır, yoksa ezdiğini sanırsın ama ezmemişsindir.

## 3. Nerede kullanılır?

- Bankacılık: hesap türleri, kredi türleri
- Sigorta: poliçe türleri (kasko, trafik, sağlık)
- Ödeme: ödeme yöntemleri (Ders 12)
- Framework'lerin kendisi: Spring'de `RuntimeException` türetmek, `HttpServlet` genişletmek

Pratikte modern Java'da kalıtım az, **interface + kompozisyon** çok kullanılır. Ama framework kodunu okuyabilmek için kalıtımı bilmek şart — yol haritası: *"kendi yazdığından 10 kat fazla başkasının kodunu okuyacaksın."*

## 4. Object sınıfı — her sınıfın atası

Java'da yazdığın her sınıf otomatik olarak `Object`'ten türer. Üç metodu hayatını değiştirir:

### toString()

```java
@Override
public String toString() {
    return "%s[%s, %s, bakiye=%s]".formatted(accountType(), accountNo, ownerName, balance);
}
```

Ezmezsen log'da `Account@6d06d69c` görürsün. Ezersen `VADESIZ[TR-1001, Ayse Yilmaz, bakiye=1000.00 TRY]`. Gece 3'te incident'a bakarken bu fark her şeydir.

### equals() ve hashCode()

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return accountNo.equals(((Account) o).accountNo);      // kimlik: hesap no
}

@Override
public int hashCode() { return Objects.hash(accountNo); }
```

İkisi birlikte ezilir. Demo çıktısı:

```
Ayni hesap no, farkli bakiye -> equals : true
HashSet boyutu (2 nesne eklendi)       : 1     ← mükerrer kayıt engellendi
```

## 5. Polymorphism'in ilk hâli: upcasting

```java
Account[] portfolio = {checking, savings, new Account(...)};

for (Account account : portfolio) {
    System.out.println(account.accountType());   // GERÇEK sınıfın metodu çalışır
}
// VADESIZ / VADELI / GENEL HESAP
```

Derleyici `Account` tipine bakar, JVM çalışma anında nesnenin gerçek sınıfına göre metodu seçer — buna **dynamic dispatch** denir. Ders 12'nin tamamı bunun üzerine kurulu.

### instanceof + pattern matching (Java 16+)

```java
if (account instanceof SavingsAccount s) {      // hem kontrol hem dönüşüm
    System.out.println("kalan cekim hakki: " + (3 - s.getWithdrawalCount()));
}
```

Ama dikkat: **kodun her yeri `instanceof` ile dolduysa tasarım yanlıştır.** Doğrusu polymorphism'dir.

## 6. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders11.InheritanceDemo
```

## 7. Sık yapılan hatalar

- Kod paylaşımı için kalıtım kurmak (IS-A yokken).
- Derin kalıtım zincirleri (A → B → C → D). 2 seviyeden fazlası genelde tasarım hatasıdır.
- `equals` ezip `hashCode` ezmemek → `HashMap`'te kaybolan nesneler.
- `super(...)` çağırmayı unutmak (üst sınıfta parametresiz constructor yoksa derlenmez).
- Constructor içinde `@Override` edilebilir metot çağırmak: alt sınıf henüz kurulmamışken çalışır, alanları `null` görür. Klasik sinsi bug.
- `protected` alanları çoğaltmak — alt sınıflar üst sınıfın iç yapısına bağımlı hâle gelir.

## 8. Mülakat notu

> "Kompozisyon mu kalıtım mı?"

Varsayılan cevap kompozisyondur ("favor composition over inheritance"). Kalıtım güçlü ama sıkı bir bağdır: üst sınıfın iç işleyişini değiştirdiğinde tüm alt sınıflar etkilenir (fragile base class problemi). Kalıtımı gerçek IS-A ve kararlı bir API varken kullan.

## 9. Alıştırma

1. `Account`'a `applyMonthlyFee()` ekle: vadesizde 15 TL, vadelide 0 TL, genel hesapta 5 TL.
2. `Employee` hiyerarşisi kur: `Employee` → `Engineer`, `Manager`. `calculateSalary()` metodu farklı çalışsın (mühendiste kıdem çarpanı, yöneticide ekip büyüklüğü primi).
3. `equals`/`hashCode`'u kaldırıp demo'yu tekrar çalıştır; `HashSet` boyutunun neden 2 olduğunu açıkla.
4. `SavingsAccount.canWithdraw` metodu içinde sayaç artırıyor — bu bir yan etki ve tasarım kokusu. Sayacı `withdraw` akışına taşıyacak bir öneri yaz (ipucu: üst sınıfa `afterWithdraw()` kancası ekle).

---
**Önceki:** [Ders 10](ders-10-encapsulation-ve-record.md) | **Sonraki:** [Ders 12 — Polymorphism ve abstract](ders-12-polymorphism-ve-abstract.md)
