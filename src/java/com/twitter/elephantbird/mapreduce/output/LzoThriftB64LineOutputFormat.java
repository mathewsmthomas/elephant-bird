package com.twitter.elephantbird.mapreduce.output;

import java.io.IOException;

import com.twitter.elephantbird.mapreduce.io.ThriftConverter;
import com.twitter.elephantbird.mapreduce.io.ThriftWritable;
import com.twitter.elephantbird.util.ThriftUtils;
import com.twitter.elephantbird.util.TypeRef;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.thrift.TBase;

/**
 * Data is written as one base64 encoded serialized thrift per line. <br><br>
 *
 * Do not use LzoThriftB64LineOutputFormat.class directly for setting
 * OutputFormat class for a job. Use getOutputFormatClass() instead.
 */
public class LzoThriftB64LineOutputFormat<M extends TBase<?, ?>>
    extends LzoOutputFormat<M, ThriftWritable<M>> {

  public LzoThriftB64LineOutputFormat() {}

  @SuppressWarnings("unchecked")
  public static <M extends TBase<?, ?>> Class<LzoThriftB64LineOutputFormat>
     getOutputFormatClass(Class<M> thriftClass, Configuration jobConf) {

    ThriftUtils.setClassConf(jobConf, LzoThriftB64LineOutputFormat.class, thriftClass);
    return LzoThriftB64LineOutputFormat.class;
  }

  public RecordWriter<NullWritable, ThriftWritable<M>> getRecordWriter(TaskAttemptContext job)
      throws IOException, InterruptedException {

    TypeRef<M> typeRef = ThriftUtils.getTypeRef(job.getConfiguration(), LzoThriftB64LineOutputFormat.class);
    return new LzoBinaryB64LineRecordWriter<M, ThriftWritable<M>>(new ThriftConverter<M>(typeRef), getOutputStream(job));
  }
}
