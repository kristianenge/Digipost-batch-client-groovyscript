/**
 * Created by kristianenge on 10.02.2016.
 */

import digipost.batch.groovy.Config
import digipost.batch.groovy.TestUtil

TestUtil testUtil = new TestUtil()
def source = this.getClass().getResource('resources/source.csv').path

def config = new Config(Avsender_id:1234)

testUtil.Test(source,config,true,false)