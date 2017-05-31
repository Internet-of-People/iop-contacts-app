package org.fermat.redtooth.can;

import org.fermat.redtooth.can.impl.*;
import org.fermat.redtooth.can.impl.Variant;
import org.junit.Test;

/**
 * Created by furszy on 5/29/17.
 */

public class CanTest {

    @Test
    public void getProfileDateAttributeTest(){
        final long timestamp = System.currentTimeMillis();
        Profile.Attribute profileDate = new Profile.Attribute("timestamp",new Variant.Uint64(timestamp));
        VariantVisitor variantVisitor = new VariantVisitorImp() {
            @Override
            public void visitLong(long value) {
                assert value==timestamp:"We don't rock!";
            }
        };
        profileDate.getValue().accept(variantVisitor);
    }



}
