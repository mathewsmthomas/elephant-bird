package com.twitter.elephantbird.pig.load;

import java.io.IOException;

import org.apache.pig.ExecType;
import org.apache.pig.backend.datastorage.DataStorage;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.util.Pair;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.elephantbird.mapreduce.io.ThriftBlockReader;
import com.twitter.elephantbird.pig.piggybank.ThriftToPig;
import com.twitter.elephantbird.util.ThriftUtils;
import com.twitter.elephantbird.util.TypeRef;


public class LzoThriftBlockPigLoader<M extends TBase<?, ?>> extends LzoBaseLoadFunc {
  private static final Logger LOG = LoggerFactory.getLogger(LzoThriftBlockPigLoader.class);

  private final TypeRef<M> typeRef_;
  private final ThriftToPig<M> thriftToPig_;
  private ThriftBlockReader<M> reader_;

  private Pair<String, String> thriftStructsRead;
  private Pair<String, String> thriftErrors;

  public LzoThriftBlockPigLoader(String thriftClassName) {
    typeRef_ = ThriftUtils.getTypeRef(thriftClassName);
    thriftToPig_ =  ThriftToPig.newInstance(typeRef_);

    String group = "LzoBlocks of " + typeRef_.getRawClass().getName();
    thriftStructsRead = new Pair<String, String>(group, "Thrift Structs Read");
    thriftErrors = new Pair<String, String>(group, "Errors");

    setLoaderSpec(getClass(), new String[]{thriftClassName});
  }

  @Override
  public void postBind() throws IOException {
    reader_ = new ThriftBlockReader<M>(is_, typeRef_);
  }

  @Override
  public void skipToNextSyncPoint(boolean atFirstRecord) throws IOException {
    // We want to explicitly not do any special syncing here, because the reader_
    // handles this automatically.
  }

  @Override
  protected boolean verifyStream() throws IOException {
    return is_ != null;
  }

  /**
   * Return every non-null line as a single-element tuple to Pig.
   */
  public Tuple getNext() throws IOException {
    if (!verifyStream()) {
      return null;
    }

    // If we are past the end of the file split, tell the reader not to read any more new blocks.
    // Then continue reading until the last of the reader's already-parsed values are used up.
    // The next split will start at the next sync point and no records will be missed.
    if (is_.getPosition() > end_) {
      reader_.markNoMoreNewBlocks();
    }

    M value;
    while ((value = reader_.readNext()) != null) {
      try {
        Tuple t = thriftToPig_.getPigTuple(value);
        incrCounter(thriftStructsRead, 1L);
        return t;
      } catch (TException e) {
        incrCounter(thriftErrors, 1L);
        LOG.warn("ThriftToTuple error :", e); // may be corrupt data.
        // try next
      }
    }
    return null;
  }

  @Override
  public Schema determineSchema(String filename, ExecType execType, DataStorage store) throws IOException {
    return ThriftToPig.toSchema(typeRef_.getRawClass());
  }
}
