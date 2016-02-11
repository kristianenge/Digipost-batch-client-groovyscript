/**
 * Created by kristianenge on 10.02.2016.
 */

import digipost.batch.groovy.Config
import digipost.batch.groovy.TestUtil

TestUtil testUtil = new TestUtil()

def config = new Config(Avsender_id:1234)

testUtil.Test(config,true,false)