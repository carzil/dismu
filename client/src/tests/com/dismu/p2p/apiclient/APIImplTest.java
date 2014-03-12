package com.dismu.p2p.apiclient;

import org.junit.Test;

public class APIImplTest {
    @Test
    public void testGetNeighbours() throws Exception {
        API api = new APIImpl();
        api.register("alpha", "alpha");
        api.register("beta", "alpha");
        api.register("gamma", "beta");

        assert(api.getNeighbours("alpha").length == 2);
        assert(api.getNeighbours("gamma").length == 1);

        api.unregister("alpha");
        api.unregister("beta");
        api.unregister("gamma");
    }

    @Test
    public void testRegister() throws Exception {
        API api = new APIImpl();
        assert(api.getNeighbours("alpha").length == 0);
        api.register("alpha", "alpha");
        assert(api.getNeighbours("alpha").length == 1);
        api.unregister("alpha");
    }

    @Test
    public void testUnregister() throws Exception {
        API api = new APIImpl();
        api.register("alpha", "alpha");
        assert(api.getNeighbours("alpha").length == 1);
        api.unregister("alpha");
        assert(api.getNeighbours("alpha").length == 0);
    }
}
