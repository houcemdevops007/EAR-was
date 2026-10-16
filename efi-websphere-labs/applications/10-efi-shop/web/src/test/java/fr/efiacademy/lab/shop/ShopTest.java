package fr.efiacademy.lab.shop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ShopTest {

    @Test
    public void catalogHasAtLeastTwelveProductsWithImages() {
        assertTrue(Catalog.all().size() >= 12);
        Set<String> ids = new HashSet<String>();
        for (Product p : Catalog.all()) {
            assertTrue("id unique " + p.getId(), ids.add(p.getId()));
            assertTrue(Catalog.categories().containsKey(p.getCategory()));
            assertTrue(p.getPriceCents() > 0);
            assertTrue(p.getImage().matches("[a-z-]+"));
        }
    }

    @Test
    public void searchIsAccentInsensitiveAndFiltersByCategory() {
        assertEquals(1, Catalog.search("ecran", null).size());
        assertEquals("p04", Catalog.search("ÉCRAN", "").get(0).getId());
        assertEquals(3, Catalog.search("", "reseau").size());
        assertEquals(0, Catalog.search("ecran", "goodies").size());
        assertEquals(Catalog.all().size(), Catalog.search(null, null).size());
    }

    @Test
    public void moneyFormatting() {
        assertEquals("0,00\u00a0€", Money.format(0));
        assertEquals("5,90\u00a0€", Money.format(590));
        assertEquals("2\u202f499,00\u00a0€", Money.format(249900));
        assertEquals("1\u202f234\u202f567,89\u00a0€", Money.format(123456789));
        assertEquals(2000, Money.vatIncluded(12000));
    }

    @Test
    public void cartAddsCapsAndComputesTotals() {
        Cart c = new Cart();
        assertEquals(3, c.add("p13", 3));
        assertEquals(3 * 590, c.getSubtotalCents());
        assertEquals("frais de port sous 100 €", Cart.SHIPPING_CENTS, c.getShippingCents());
        assertEquals(c.getSubtotalCents() + Cart.SHIPPING_CENTS, c.getTotalCents());
        assertEquals(2, c.add("p11", 2));
        assertEquals("quantité plafonnée à 10", 10, c.add("p11", 50));
        assertEquals(13, c.getCount());
        assertEquals(10 * 1490 + 3 * 590, c.getSubtotalCents());
        assertEquals("livraison offerte au-delà de 100 €", 0, c.getShippingCents());
        c.add("p01", 1);
        assertEquals(6, c.set("p01", 99));
        assertEquals(0, c.set("p13", 0));
        assertTrue(c.remove("p11"));
        assertFalse(c.remove("p11"));
        c.clear();
        assertTrue(c.isEmpty());
        assertEquals(0, c.getShippingCents());
    }

    @Test
    public void cartRejectsUnknownProductsAndBadQuantities() {
        Cart c = new Cart();
        try {
            c.add("p99", 1);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("Produit inconnu.", expected.getMessage());
        }
        try {
            c.add("p01", 0);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("Quantité invalide.", expected.getMessage());
        }
        try {
            c.set("p01", -1);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("Quantité invalide.", expected.getMessage());
        }
    }

    @Test
    public void cartIsSerializableForSessionReplication() throws Exception {
        Cart c = new Cart();
        c.add("p08", 2);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(c);
        oos.close();
        Cart back = (Cart) new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray())).readObject();
        assertEquals(2, back.getCount());
        assertEquals(9000, back.getSubtotalCents());
    }

    @Test
    public void checkoutValidation() {
        Map<String, String> raw = new HashMap<String, String>();
        raw.put("name", " A ");
        raw.put("email", "pas-un-email");
        raw.put("zip", "7500");
        Map<String, String> errors = Order.validate(Order.clean(raw));
        assertEquals(6, errors.size());

        raw.put("name", "Candidate EFI");
        raw.put("email", "candidate@efi-academy.fr");
        raw.put("address", "12 rue de la Cellule");
        raw.put("zip", "75011");
        raw.put("city", "Paris");
        raw.put("terms", "on");
        raw.put("shipping", "express");
        Map<String, String> form = Order.clean(raw);
        assertTrue(Order.validate(form).isEmpty());

        Cart c = new Cart();
        c.add("p06", 2);
        Order o = Order.place(form, c, 1760000000000L);
        assertTrue(o.getReference().matches("EFI-\\d{8}-[A-Z2-9]{5}"));
        assertEquals(4980, o.getSubtotalCents());
        assertEquals(Cart.SHIPPING_CENTS + 900, o.getShippingCents());
        assertEquals(1, o.getLines().size());
        assertEquals("2", o.getLines().get(0)[1]);
    }

    @Test
    public void cartJsonIsWellFormed() {
        Cart c = new Cart();
        c.add("p03", 1);
        String json = ShopSupport.cartJson(c);
        assertTrue(json.contains("\"count\":1"));
        assertTrue(json.contains("\"productId\":\"p03\""));
        assertTrue(json.contains("\"shipping\":\"Offerte\""));
    }
}
