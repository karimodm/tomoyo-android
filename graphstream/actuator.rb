#!/usr/bin/ruby

require 'file/tail'
require 'mechanize'

if ARGV.count != 2 then abort('actuator.rb URL FILE') end

class MMechanize < Mechanize
	def initialize(url)
		@url = url
		@sync = Mutex.new
		super()
	end
	def get(params = {})
		@sync.synchronize {
			super(@url, params)
		}
	end
end

def add_node(id, label, is_app = true)
	llabel = /([^\.\/]+)$/.match(label)[1]
	$agent.get({ 'q' => 'an', 'id' => id })
	$agent.get({ 'q' => 'ana', 'id' => id, 'key' => 'ui.label', 'value' => llabel })
	$agent.get({ 'q' => 'ana', 'id' => id, 'key' => 'ui.class', 'value' => is_app ? 'app' : 'service' })
end

def add_edge(idf, idt)
	id = "#{idf}E#{idt}"
	idx = $edges.find_index(id)
	unless idx
		$edges.push WeightedElement.new(id)
		$agent.get({ 'q' => 'ae', 'id' => id, 'from' => idf, 'to' => idt, 'directed' => 1 })
	else
		$edges[idx].weight += 1
	end
	edge = idx ? $edges[idx] : $edges.last
	$agent.get({ 'q' => 'aea', 'id' => id, 'key' => 'ui.label', 'value' => "-#{edge.weight}-" })
	$agent.get({ 'q' => 'aea', 'id' => id, 'key' => 'weight', 'value' => edge.weight })
end

class Domain
	attr_reader :id
	@@count = 1
	def initialize(name)
		@name = name
		@services = Array.new
		@id =  @@count
		@@count += 1
		add_node(@id, @name)
	end
	def transaction_received(toserv)
		idx = @services.find_index(toserv)
		unless idx
			@services.push(toserv)
			sid = "#{@id}.#{@services.count}"
			add_node(sid, toserv, false)
		else
			sid = "#{@id}.#{idx + 1}"
		end
		add_edge(@id, sid)
	end
	def ==(s)
		@name == s
	end
end

class WeightedElement
	attr_accessor :weight
	def initialize(id)
		@weight = 1
		@id = id
	end
	def ==(s)
		@id == s
	end
end

class DomainCollection
	attr_reader :domains
	def initialize
		@domains = Array.new
	end
	def push(dom)
		idx = @domains.find_index(dom)
		return @domains[idx] if idx
		@domains.push Domain.new(dom)
		@domains.last
	end
end

class AuditMonitor < File
	include File::Tail
	attr_reader :collection
	def initialize(filename)
		@reg = /^(ipc android_binder_transaction )([^ ]+) (.+)$/
		@collection = DomainCollection.new
		super
	end
	def push_line(line)
		return if line =~ /^#/
		if (line =~ /^</)
			@domain = line
			return
		end
		return if not line =~ @reg
		process(@domain, $3, $2)
	end
	def process(from, dest, service)
		dfrom = @collection.push(from)
		ddest = @collection.push(dest)
		add_edge(dfrom.id, ddest.id)
		ddest.transaction_received(service)
		$ipc_processed += 1
	end
end

def algo
	puts 'Centrality...'
	$stdin.gets
	$agent.get({ 'q' => 'centrality' })
	puts 'Weight Edge-Coloring...'
	$stdin.gets
	$agent.get({ 'q' => 'edgecoloring' })
	puts 'Spanning Tree...'
	$stdin.gets
	$agent.get({ 'q' => 'spantree' })
	puts 'DONE'
end

$agent = MMechanize.new(ARGV[0])
$edges = Array.new
io = AuditMonitor.new(ARGV[1])

$ipc_processed = 0
Signal.trap('SIGUSR1') {
	puts "IPC Processed: #{$ipc_processed}"
}

Thread.new { algo }
io.tail { |line| io.push_line(line.chomp!) }
